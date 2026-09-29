/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinksTest {

    @Test
    fun parsesStorefrontAppLink() {
        assertEquals("my-app", DeepLinks.parseAppId("https://shizustore.com/apps/my-app"))
    }

    @Test
    fun parsesCustomSchemeLink() {
        assertEquals("my-app", DeepLinks.parseAppId("shizustore://apps/my-app"))
    }

    @Test
    fun parsesPackageParameterOnCustomScheme() {
        assertEquals(
            "io.nekohasekai.sfa",
            DeepLinks.parseAppId("shizustore://apps/?package=io.nekohasekai.sfa")
        )
    }

    @Test
    fun packageParameterWinsOverPathSlug() {
        assertEquals(
            "com.example.app",
            DeepLinks.parseAppId("shizustore://apps/my-app?package=com.example.app")
        )
    }

    @Test
    fun fallsBackToPathSlugOnInvalidPackage() {
        assertEquals(
            "my-app",
            DeepLinks.parseAppId("shizustore://apps/my-app?package=not-a-package")
        )
        assertEquals("my-app", DeepLinks.parseAppId("shizustore://apps/my-app?package="))
        assertEquals("my-app", DeepLinks.parseAppId("shizustore://apps/my-app?package=a..b"))
    }

    @Test
    fun ignoresOtherQueryParameters() {
        assertEquals("my-app", DeepLinks.parseAppId("shizustore://apps/my-app?utm_source=x"))
    }

    @Test
    fun firstPackageParameterWins() {
        assertEquals("a.b", DeepLinks.parseAppId("shizustore://apps/?package=a.b&package=c.d"))
    }

    @Test
    fun decodesPercentEncodedPackage() {
        assertEquals(
            "com.example.app",
            DeepLinks.parseAppId("shizustore://apps/?package=com.example%2Eapp")
        )
    }

    @Test
    fun ignoresPackageParameterOnStorefrontLinks() {
        assertEquals(
            "my-app",
            DeepLinks.parseAppId("https://shizustore.com/apps/my-app?package=com.example.app")
        )
    }

    @Test
    fun trimsTrailingSlash() {
        assertEquals("my-app", DeepLinks.parseAppId("https://shizustore.com/apps/my-app/"))
    }

    @Test
    fun ignoresQueryAndFragment() {
        assertEquals(
            "my-app",
            DeepLinks.parseAppId("https://shizustore.com/apps/my-app?utm_source=x#section")
        )
    }

    @Test
    fun acceptsHostCaseInsensitively() {
        assertEquals("my-app", DeepLinks.parseAppId("https://ShizuStore.com/apps/my-app"))
    }

    @Test
    fun rejectsForeignHost() {
        assertNull(DeepLinks.parseAppId("https://example.com/apps/my-app"))
    }

    @Test
    fun rejectsOtherPaths() {
        assertNull(DeepLinks.parseAppId("https://shizustore.com/other/my-app"))
        assertNull(DeepLinks.parseAppId("https://shizustore.com/apps"))
        assertNull(DeepLinks.parseAppId("https://shizustore.com/apps/"))
    }

    @Test
    fun rejectsNestedPath() {
        assertNull(DeepLinks.parseAppId("https://shizustore.com/apps/my-app/extra"))
        assertNull(DeepLinks.parseAppId("shizustore://apps/my-app/extra"))
    }

    @Test
    fun rejectsForeignScheme() {
        assertNull(DeepLinks.parseAppId("http://shizustore.com/apps/my-app"))
        assertNull(DeepLinks.parseAppId("shizustore://other/my-app"))
        assertNull(DeepLinks.parseAppId("my-app"))
    }

    @Test
    fun toleratesNullOrGarbage() {
        assertNull(DeepLinks.parseAppId(null))
        assertNull(DeepLinks.parseAppId(""))
        assertNull(DeepLinks.parseAppId("   "))
        assertNull(DeepLinks.parseAppId("not a uri at all"))
    }
}
