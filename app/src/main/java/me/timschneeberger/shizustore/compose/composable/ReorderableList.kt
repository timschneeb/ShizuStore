/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The order shown during a drag is held here rather than in the store behind the list: writing to
 * the database flow on every boundary crossing would race the drag, so the new order goes out once
 * on drop and is held until the store catches up.
 */
class ReorderState<T> internal constructor(
    private val listState: LazyListState,
    private val idOf: (T) -> Int,
    private val onReordered: (List<T>) -> Unit
) {
    /** The list to draw while a drag is in flight or still settling, otherwise null. */
    var order: List<T>? by mutableStateOf(null)
        private set

    var draggedId: Int? by mutableStateOf(null)
        private set

    var offset: Float by mutableFloatStateOf(0f)
        private set

    /** A plain field on purpose: written during composition and read only from a gesture
     * callback, so it must not invalidate anything. */
    internal var source: List<T> = emptyList()

    fun start(id: Int) {
        order = source
        draggedId = id
        offset = 0f
    }

    /** Returns true when the row changed slots, which is the moment worth a haptic tick. */
    fun drag(delta: Float): Boolean {
        val items = order ?: return false
        val id = draggedId ?: return false
        offset += delta

        val visible = listState.layoutInfo.visibleItemsInfo
        val dragged = visible.firstOrNull { it.key == id } ?: return false
        val from = items.indexOfFirst { idOf(it) == id }
        if (from < 0) return false

        val centre = dragged.offset + dragged.size / 2f + offset
        val target = visible.firstOrNull { candidate ->
            candidate.key != id &&
                centre.toInt() in candidate.offset..(candidate.offset + candidate.size)
        } ?: return false

        val to = items.indexOfFirst { idOf(it) == target.key }
        if (to < 0) return false

        order = items.toMutableList().apply { add(to, removeAt(from)) }
        offset -= (target.offset - dragged.offset)
        return true
    }

    fun stop() {
        order?.let(onReordered)
        draggedId = null
        offset = 0f
    }

    fun cancel() {
        order = null
        draggedId = null
        offset = 0f
    }

    /** Hands the list back to [items] once they agree, or when membership changes and the held
     * order is stale. */
    internal fun settle(items: List<T>) {
        if (draggedId != null) return
        val held = (order ?: return).map(idOf)
        val current = items.map(idOf)
        if (held == current || held.toSet() != current.toSet()) order = null
    }
}

@Composable
fun <T> rememberReorderState(
    listState: LazyListState,
    items: List<T>,
    idOf: (T) -> Int,
    onReordered: (List<T>) -> Unit
): ReorderState<T> {
    val state = remember(listState) { ReorderState(listState, idOf, onReordered) }
    state.source = items
    state.settle(items)
    return state
}

/** Keyed on the row's id alone: keying on the list would restart the gesture the moment the drag
 * reordered it. */
@Composable
fun <T> Modifier.dragHandle(state: ReorderState<T>, id: Int): Modifier {
    val haptics = LocalHapticFeedback.current

    return pointerInput(id) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                state.start(id)
            },
            onDrag = { _, dragAmount ->
                if (state.drag(dragAmount.y)) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                }
            },
            onDragEnd = {
                haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                state.stop()
            },
            onDragCancel = { state.cancel() }
        )
    }
}
