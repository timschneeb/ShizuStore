/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import me.timschneeberger.shizustore.data.api.Availability
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceLauncherTest {
    @Test
    fun directApkRoutesToInstaller() {
        assertEquals(SourceTarget.Apk, sourceTarget(Availability.DIRECT_APK, null, null, null))
    }

    @Test
    fun playRedirectPrefersStoreUrl() {
        val storeUrl = "https://play.google.com/store/apps/details?id=x"
        assertEquals(
            SourceTarget.Play(storeUrl),
            sourceTarget(Availability.PLAY_REDIRECT, storeUrl, null, null)
        )
    }

    @Test
    fun playRedirectWithoutStoreUrlIsNone() {
        assertEquals(
            SourceTarget.None,
            sourceTarget(Availability.PLAY_REDIRECT, null, "https://example.com", null)
        )
    }

    @Test
    fun linkOnlyPrefersUrlThenSourceUrl() {
        assertEquals(
            SourceTarget.Link("https://example.com"),
            sourceTarget(Availability.LINK_ONLY, null, "https://example.com", "https://fallback")
        )
        assertEquals(
            SourceTarget.Link("https://fallback"),
            sourceTarget(Availability.LINK_ONLY, null, null, "https://fallback")
        )
    }

    @Test
    fun linkOnlyWithoutTargetIsNone() {
        assertEquals(SourceTarget.None, sourceTarget(Availability.LINK_ONLY, null, null, null))
    }

    @Test
    fun excludedIsNone() {
        assertEquals(
            SourceTarget.None,
            sourceTarget(
                Availability.EXCLUDED,
                "https://play.google.com/store/apps/details?id=x",
                "https://example.com",
                null
            )
        )
    }
}
