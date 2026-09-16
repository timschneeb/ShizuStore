/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import me.timschneeberger.shizustore.R

/**
 * Adaptive (full-bleed) icons are framed as a rounded square; legacy rasters and
 * letter avatars get a softer squircle.
 */
@Composable
fun appIconShape(adaptive: Boolean): Shape {
    val radius = dimensionResource(R.dimen.radius_medium)
    return remember(adaptive, radius) {
        if (adaptive) RoundedCornerShape(radius) else RoundedCornerShape(percent = SQUIRCLE_PERCENT)
    }
}

private const val SQUIRCLE_PERCENT = 30
