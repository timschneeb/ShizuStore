/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val REMOVE_ANIM_DURATION_MS = 300L

@Composable
fun RemovableListItem(
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (triggerRemove: () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(true) }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        exit = shrinkVertically() + fadeOut()
    ) {
        content {
            scope.launch {
                visible = false
                delay(REMOVE_ANIM_DURATION_MS)
                onRemove()
            }
        }
    }
}
