/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.theme.motionEffectsSpec
import me.timschneeberger.shizustore.compose.theme.motionSpatialSpec

@Composable
fun AnimatedAppIcon(
    modifier: Modifier = Modifier,
    iconUrl: String,
    progress: Float = 0F,
    inProgress: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = motionEffectsSpec(),
        label = "appIconProgress"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (inProgress) IN_PROGRESS_SCALE else 1F,
        animationSpec = motionSpatialSpec(),
        label = "appIconScale"
    )
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val restingRadius = dimensionResource(R.dimen.radius_medium)
    // Morph the rounding up to a full circle instead of swapping the shape,
    // which jumped mid-scale before.
    val cornerRadius by animateDpAsState(
        targetValue = if (inProgress) {
            with(density) { size.width.toDp() / 2 }
        } else {
            restingRadius
        },
        animationSpec = motionSpatialSpec(),
        label = "appIconCorner"
    )
    val clip = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    Box(
        modifier = modifier.onSizeChanged { size = it },
        contentAlignment = Alignment.Center
    ) {
        if (inProgress) {
            val indicatorModifier = Modifier
                .fillMaxSize()
                .testTag(PROGRESS_INDICATOR_TAG)
            // Branch on the target, not the animated value: reading the animation
            // here would recompose the icon on every frame.
            if (progress > 0f) {
                CircularProgressIndicator(
                    modifier = indicatorModifier,
                    progress = { animatedProgress / PERCENT }
                )
            } else {
                CircularProgressIndicator(modifier = indicatorModifier)
            }
        }

        AsyncImage(
            model = rememberAppIconModel(iconUrl),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .clip(clip)
        )
    }
}

private const val PROGRESS_INDICATOR_TAG = "progressIndicator"

private const val IN_PROGRESS_SCALE = 0.75F
private const val PERCENT = 100F
