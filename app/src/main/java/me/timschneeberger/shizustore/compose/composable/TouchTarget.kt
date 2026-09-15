/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import me.timschneeberger.shizustore.R

/**
 * Holds a tappable row to the 48dp minimum target that a short one-line row would otherwise miss.
 */
@Composable
fun Modifier.minTouchTarget(): Modifier =
    defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target))
