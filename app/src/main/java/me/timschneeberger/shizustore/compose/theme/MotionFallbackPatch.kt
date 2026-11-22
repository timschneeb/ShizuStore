/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.theme

import android.util.Log
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset

/**
 * Replaces Compose's fallback animation for interrupted enter/exit slides.
 *
 * The enter/exit modifier honors the slide spring passed by the app only while
 * a segment runs its canonical direction. An interrupted slide (flipping a tab
 * back mid way, popping a screen while it is still entering) falls back to a
 * hardcoded, critically damped default spring, which reads as a different,
 * non-expressive animation. That fallback is a private static field, so the
 * only way to keep interrupted slides on the motion scheme's spatial curve is
 * to swap the field for the scheme's own spring.
 *
 * The lookup is best effort: if a future Compose renames the field, the patch
 * logs once and the app keeps the framework behavior. The ProGuard rule in
 * app/proguard-rules.pro keeps the field name in release builds.
 */
internal object MotionFallbackPatch {
    private const val TAG = "MotionFallbackPatch"
    private const val FIELD_CLASS = "androidx.compose.animation.EnterExitTransitionKt"
    private const val FIELD_NAME = "DefaultOffsetAnimationSpec"

    private var applied: Boolean? = null
    private var warned = false

    fun install(expressive: Boolean) {
        if (applied == expressive) return
        applied = expressive

        val spec: FiniteAnimationSpec<IntOffset> = spring(
            dampingRatio = if (expressive) 0.8f else 0.9f,
            stiffness = if (expressive) 380f else 700f,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )
        val failure = runCatching {
            val field = Class.forName(FIELD_CLASS).getDeclaredField(FIELD_NAME)
            field.isAccessible = true
            field.set(null, spec)
        }.exceptionOrNull()

        if (failure != null && !warned) {
            warned = true
            // Not mocked in JVM unit tests, so even the warning stays best effort.
            runCatching {
                Log.w(TAG, "Compose interrupted-slide fallback patch unavailable", failure)
            }
        }
    }
}
