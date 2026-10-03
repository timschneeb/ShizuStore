/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Motion vocabulary for custom animations. Specs come from the theme's
 * MotionScheme so bespoke motion stays in step with Material components under
 * MaterialExpressiveTheme.
 */

/** Default motion for shape, position and size changes. */
@Composable
@ReadOnlyComposable
internal fun <T> motionSpatialSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.defaultSpatialSpec()

/** Fast motion for small spatial accents, for example an expanding chevron. */
@Composable
@ReadOnlyComposable
internal fun <T> motionFastSpatialSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.fastSpatialSpec()

/** Default motion for color and alpha changes. */
@Composable
@ReadOnlyComposable
internal fun <T> motionEffectsSpec(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.defaultEffectsSpec()
