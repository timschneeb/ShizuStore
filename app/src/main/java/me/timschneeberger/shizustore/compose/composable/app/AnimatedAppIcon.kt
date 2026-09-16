/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import coil3.compose.AsyncImage
import me.timschneeberger.shizustore.R

@Composable
fun AnimatedAppIcon(
    modifier: Modifier = Modifier,
    iconUrl: String,
    progress: Float = 0F,
    inProgress: Boolean = false
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = ANIM_DURATION_MS)
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (inProgress) IN_PROGRESS_SCALE else 1F,
        animationSpec = tween(durationMillis = ANIM_DURATION_MS)
    )
    val clip = if (inProgress) {
        CircleShape
    } else {
        val radius = dimensionResource(R.dimen.radius_medium)
        remember(radius) { RoundedCornerShape(radius) }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
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

private const val ANIM_DURATION_MS = 300
private const val IN_PROGRESS_SCALE = 0.75F
private const val PERCENT = 100F
