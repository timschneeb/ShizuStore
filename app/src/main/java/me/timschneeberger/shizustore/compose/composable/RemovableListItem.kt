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
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.compose.theme.motionEffectsSpec
import me.timschneeberger.shizustore.compose.theme.motionSpatialSpec

private const val REMOVE_ANIM_DURATION_MS = 300L

@Composable
fun RemovableListItem(
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (triggerRemove: () -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(true) }
    val shrinkSpec = motionSpatialSpec<IntSize>()
    val exitFadeSpec = motionEffectsSpec<Float>()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        exit = shrinkVertically(shrinkSpec) + fadeOut(exitFadeSpec)
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
