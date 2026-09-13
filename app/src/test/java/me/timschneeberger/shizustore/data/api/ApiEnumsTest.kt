/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiEnumsTest {
    @Test
    fun mapsKnownWireValues() {
        assertEquals(Availability.DIRECT_APK, Availability.fromWire("direct_apk"))
        assertEquals(Availability.PLAY_REDIRECT, Availability.fromWire("play_redirect"))
        assertEquals(Availability.LINK_ONLY, Availability.fromWire("link_only"))
        assertEquals(Listing.CLOSED_SOURCE, Listing.fromWire("closed_source"))
        assertEquals(AppType.LIBRARY, AppType.fromWire("library"))
        assertEquals(CategorySection.MISC, CategorySection.fromWire("misc"))
        assertEquals(SourceKind.CODEBERG, SourceKind.fromWire("codeberg"))
    }

    @Test
    fun isCaseAndWhitespaceInsensitive() {
        assertEquals(Availability.DIRECT_APK, Availability.fromWire("  DIRECT_APK "))
        assertEquals(SourceKind.GITHUB, SourceKind.fromWire("GitHub"))
    }

    @Test
    fun unknownOrNullMapsToNull() {
        assertNull(Availability.fromWire("future_value"))
        assertNull(Availability.fromWire(null))
        assertNull(SourceKind.fromWire(""))
    }
}
