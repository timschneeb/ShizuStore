/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCandidateTest {

    @Test
    fun universalCandidateRunsOnAnyDevice() {
        assertTrue(candidate(abi = null).supportsAbi(listOf("x86")))
    }

    @Test
    fun fromKeepsTheCandidatesOwnFlavorPackage() {
        val entity = AppDownloadEntity(
            appSlug = "app",
            packageName = "com.app.play",
            apkUrl = "https://example/play.apk",
            sigKey = "k"
        )
        assertEquals("com.app.play", AppCandidate.from(entity, "com.app").packageName)
    }

    @Test
    fun fromFallsBackToTheAppPackageForLegacyRows() {
        val entity = AppDownloadEntity(
            appSlug = "app",
            apkUrl = "https://example/app.apk",
            sigKey = "k"
        )
        assertEquals("com.app", AppCandidate.from(entity, "com.app").packageName)
    }

    @Test
    fun matchesWhenDeviceSupportsAbi() {
        assertTrue(candidate(abi = "arm64-v8a").supportsAbi(listOf("armeabi-v7a", "arm64-v8a")))
    }

    @Test
    fun rejectsUnsupportedAbi() {
        assertFalse(candidate(abi = "x86").supportsAbi(listOf("arm64-v8a")))
    }

    /** Off-device the ABI list is empty, so filtering must be skipped rather than hide everything. */
    @Test
    fun emptySupportedListDoesNotFilter() {
        assertTrue(candidate(abi = "x86").supportsAbi(emptyList()))
    }

    @Test
    fun supportsAnyTokenOfCommaSeparatedAbiList() {
        assertTrue(candidate(abi = "armeabi-v7a,arm64-v8a").supportsAbi(listOf("arm64-v8a")))
        assertTrue(candidate(abi = "armeabi-v7a, arm64-v8a").supportsAbi(listOf("ARM64-V8A")))
        assertTrue(candidate(abi = "armeabi-v7a arm64-v8a").supportsAbi(listOf("arm64-v8a")))
        assertFalse(candidate(abi = "armeabi-v7a,x86").supportsAbi(listOf("arm64-v8a")))
    }

    private fun candidate(abi: String?): AppCandidate = AppCandidate(
        id = 1,
        appSlug = "app",
        packageName = "com.app",
        versionCode = 1,
        versionName = "1.0",
        source = null,
        apkUrl = "https://example/app.apk",
        archiveEntry = null,
        size = 10,
        sha256 = null,
        sigSha256 = null,
        sigMd5 = null,
        minSdk = 24,
        abi = abi,
        isPrimary = true
    )
}
