/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class UserAgentTest {

    @Test
    fun combinesAppNameAndVersion() {
        assertEquals("Shizu Store/0.1.0", userAgent("Shizu Store", "0.1.0"))
    }
}
