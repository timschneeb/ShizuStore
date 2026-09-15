/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import me.timschneeberger.shizustore.R

/** Shared so one transition drives the whole placeholder; a transition per block stutters
 * badly on a full screen of rows. */
private val LocalShimmerOffset = staticCompositionLocalOf<State<Float>> {
    error("Shimmer blocks must be wrapped in ShimmerHost")
}

@Composable
internal fun ShimmerHost(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    CompositionLocalProvider(LocalShimmerOffset provides offset, content = content)
}

@Composable
internal fun ShimmerBlock(modifier: Modifier, radiusRes: Int = R.dimen.radius_small) {
    val offset = LocalShimmerOffset.current
    val base = MaterialTheme.colorScheme.onSurface
    val colors = remember(base) {
        listOf(
            base.copy(alpha = 0.08f),
            base.copy(alpha = 0.20f),
            base.copy(alpha = 0.08f)
        )
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(dimensionResource(radiusRes)))
            .drawWithCache {
                val sweep = size.width * 1.5f
                val leading = offset.value * sweep * 2f - sweep
                val brush = Brush.linearGradient(
                    colors = colors,
                    start = Offset(leading, 0f),
                    end = Offset(leading + sweep, size.height)
                )
                onDrawBehind { drawRect(brush) }
            }
    )
}

@Composable
private fun ShimmerTextStack(modifier: Modifier = Modifier, widthFractions: List<Float>) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        widthFractions.forEach { fraction ->
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(dimensionResource(R.dimen.skeleton_line_row))
            )
        }
    }
}

@Composable
private fun ShimmerListRow(showTrailing: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = dimensionResource(R.dimen.list_item_height_three_line))
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large))
    ) {
        ShimmerBlock(
            modifier = Modifier.requiredSize(dimensionResource(R.dimen.icon_size_medium))
        )
        ShimmerTextStack(
            modifier = Modifier.weight(1f),
            widthFractions = listOf(0.7f, 0.5f, 0.5f)
        )
        if (showTrailing) {
            ShimmerBlock(
                modifier = Modifier
                    .width(dimensionResource(R.dimen.skeleton_button_width))
                    .height(dimensionResource(R.dimen.skeleton_button_height)),
                radiusRes = R.dimen.radius_large
            )
        }
    }
}

@Composable
internal fun ShimmerAppRow() {
    ShimmerListRow(showTrailing = false)
}

@Composable
internal fun ShimmerUpdateItem() {
    ShimmerListRow(showTrailing = true)
}

@Composable
internal fun ShimmerSectionHeader(clickable: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            Text(text = "", style = MaterialTheme.typography.titleMedium)
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(dimensionResource(R.dimen.skeleton_line_title))
            )
        }
        if (clickable) {
            Spacer(modifier = Modifier.size(dimensionResource(R.dimen.icon_size_default)))
        }
    }
}

@Composable
internal fun ShimmerCarouselSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = dimensionResource(R.dimen.spacing_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(dimensionResource(R.dimen.skeleton_line_title))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            repeat(5) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(
                        dimensionResource(R.dimen.spacing_xsmall)
                    )
                ) {
                    ShimmerBlock(
                        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_cluster)),
                        radiusRes = R.dimen.radius_medium
                    )
                    ShimmerBlock(
                        modifier = Modifier
                            .width(dimensionResource(R.dimen.icon_size_cluster) * 0.75f)
                            .height(dimensionResource(R.dimen.skeleton_line_tile))
                    )
                    ShimmerBlock(
                        modifier = Modifier
                            .width(dimensionResource(R.dimen.icon_size_cluster) * 0.5f)
                            .height(dimensionResource(R.dimen.skeleton_line_tile))
                    )
                }
            }
        }
    }
}

@Composable
internal fun ShimmerAppTile() {
    Column(
        modifier = Modifier
            .padding(dimensionResource(R.dimen.spacing_xsmall))
            .width(dimensionResource(R.dimen.icon_size_cluster)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        ShimmerBlock(
            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_cluster)),
            radiusRes = R.dimen.radius_medium
        )
        ShimmerTextLine(MaterialTheme.typography.labelMedium, widthFraction = 0.9f)
        ShimmerTextLine(MaterialTheme.typography.labelSmall, widthFraction = 0.5f)
    }
}

@Composable
private fun ShimmerTextLine(style: TextStyle, widthFraction: Float) {
    Box(contentAlignment = Alignment.CenterStart) {
        Text(text = "", style = style)
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(dimensionResource(R.dimen.skeleton_line_tile))
        )
    }
}

@Composable
internal fun ShimmerInstallerRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = dimensionResource(R.dimen.list_item_height_three_line))
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.size(dimensionResource(R.dimen.icon_size_small)))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerTextLine(MaterialTheme.typography.bodyLarge, widthFraction = 0.45f)
            ShimmerTextLine(MaterialTheme.typography.bodySmall, widthFraction = 0.85f)
            ShimmerTextLine(MaterialTheme.typography.bodySmall, widthFraction = 0.65f)
        }
    }
}
