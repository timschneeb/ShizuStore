/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShizuUrlsTest {
    @Test
    fun buildsImmutableIconUrl() {
        assertEquals(
            "https://store.example/icons/abc123.png",
            ShizuUrls.icon("https://store.example", "abc123")
        )
    }

    @Test
    fun trimsTrailingSlashAndWhitespace() {
        assertEquals(
            "https://store.example/icons/abc123.png",
            ShizuUrls.icon("  https://store.example/  ", " abc123 ")
        )
    }

    @Test
    fun returnsNullWhenUnusable() {
        assertNull(ShizuUrls.icon("https://store.example", null))
        assertNull(ShizuUrls.icon("https://store.example", "   "))
        assertNull(ShizuUrls.icon("   ", "abc123"))
    }
}
