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
 * Holds a tappable row to the 48dp minimum target. Rows here are laid out from their text, which
 * on one line lands well under that, and a presentational control inside enforces nothing.
 */
@Composable
fun Modifier.minTouchTarget(): Modifier =
    defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target))
