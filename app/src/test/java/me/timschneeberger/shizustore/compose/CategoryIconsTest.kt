/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class CategoryIconsTest {

    private val knownSlugs = listOf(
        "ai-agents",
        "android-tv",
        "audio",
        "automation",
        "communication",
        "customization",
        "development-utilities",
        "device-owner-dpm",
        "display-management",
        "entertainment",
        "file-management",
        "games",
        "input-methods",
        "installer-app-stores",
        "miscellaneous",
        "network",
        "patching",
        "power-management",
        "privacy",
        "productivity",
        "quick-settings",
        "software-management",
        "task-manager",
        "terminals",
        "vendor-specific",
        "vendor-specific-google-pixel",
        "vendor-specific-miui",
        "vendor-specific-samsung-oneui",
        "vendor-specific-other"
    )

    @Test
    fun everyKnownSlugMapsToAnIcon() {
        val generic = Icons.Rounded.Category
        knownSlugs.filterNot { it == "miscellaneous" }.forEach { slug ->
            assertNotSame("$slug should have its own icon", generic, categoryIcon(slug))
        }
    }

    @Test
    fun unknownSlugFallsBackToTheGenericIcon() {
        assertSame(Icons.Rounded.Category, categoryIcon("does-not-exist"))
    }
}
