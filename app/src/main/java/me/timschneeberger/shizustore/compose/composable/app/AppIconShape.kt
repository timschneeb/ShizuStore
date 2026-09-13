/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import me.timschneeberger.shizustore.R

/**
 * Adaptive (full-bleed) icons are framed as a rounded square; legacy rasters and
 * letter avatars get a softer squircle.
 */
@Composable
fun appIconShape(adaptive: Boolean): Shape = if (adaptive) {
    RoundedCornerShape(dimensionResource(R.dimen.radius_medium))
} else {
    RoundedCornerShape(percent = SQUIRCLE_PERCENT)
}

private const val SQUIRCLE_PERCENT = 30
