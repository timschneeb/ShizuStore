/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseUrlProviderTest {
    @Test
    fun overrideWinsAndNormalizes() = runTest {
        val provider = BaseUrlProvider(
            readOverride = { " http://192.168.178.72:5137/ " },
            defaultValue = "https://default.example/"
        )

        assertEquals("http://192.168.178.72:5137", provider.current())
        assertTrue(provider.isConfigured())
    }

    @Test
    fun fallsBackToDefaultWhenOverrideBlank() = runTest {
        val provider = BaseUrlProvider(
            readOverride = { "   " },
            defaultValue = "https://default.example/"
        )

        assertEquals("https://default.example", provider.current())
    }

    @Test
    fun reportsUnconfiguredWhenBothBlank() = runTest {
        val provider = BaseUrlProvider(readOverride = { null }, defaultValue = "")

        assertEquals("", provider.current())
        assertFalse(provider.isConfigured())
    }
}
