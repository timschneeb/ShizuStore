/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.theme

import java.lang.reflect.Modifier
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the internal field the interrupted-slide patch depends on: a Compose
 * upgrade that renames it must fail here instead of silently falling back to
 * the non-expressive framework spring.
 */
class MotionFallbackPatchTest {
    @Test
    fun fallbackFieldExistsAndInstallNeverThrows() {
        val field = Class.forName("androidx.compose.animation.EnterExitTransitionKt")
            .getDeclaredField("DefaultOffsetAnimationSpec")
        assertTrue("fallback spec must stay a static field", Modifier.isStatic(field.modifiers))

        // The JVM refuses writes to static final fields, so the actual swap is
        // only verifiable on device; here only the never-throwing contract matters.
        MotionFallbackPatch.install(true)
    }
}
