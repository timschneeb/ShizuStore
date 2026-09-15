/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDownloadEntityTest {

    @Test
    fun sigKeyPrefersFirstSha256Token() {
        val key = AppDownloadEntity.sigKeyOf(
            sigSha256 = "AAAA BBBB",
            sigMd5 = "cccc",
            apkUrl = "https://example.com/app.apk"
        )
        assertEquals("aaaa", key)
    }

    @Test
    fun sigKeyFallsBackToMd5() {
        val key = AppDownloadEntity.sigKeyOf(
            sigSha256 = "   ",
            sigMd5 = "DDDD EEEE",
            apkUrl = "https://example.com/app.apk"
        )
        assertEquals("dddd", key)
    }

    @Test
    fun sigKeyFallsBackToUrl() {
        val key = AppDownloadEntity.sigKeyOf(
            sigSha256 = null,
            sigMd5 = null,
            apkUrl = "https://example.com/app.apk"
        )
        assertEquals("url:https://example.com/app.apk", key)
    }

    @Test
    fun sigKeyAppendsLowercasedAbiSuffix() {
        val key = AppDownloadEntity.sigKeyOf(
            sigSha256 = "AAAA",
            sigMd5 = null,
            apkUrl = "https://example.com/app.apk",
            abi = "Arm64-v8a"
        )
        assertEquals("aaaa:arm64-v8a", key)
    }

    @Test
    fun sigKeyIgnoresBlankAbi() {
        val key = AppDownloadEntity.sigKeyOf(
            sigSha256 = "AAAA",
            sigMd5 = null,
            apkUrl = "https://example.com/app.apk",
            abi = "  "
        )
        assertEquals("aaaa", key)
    }
}
