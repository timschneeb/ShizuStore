/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CertFingerprintTest {

    @Test
    fun parsesSpaceJoinedSetAndNormalizesCase() {
        val parsed = parseFingerprintSet(" AAA bbb\tCCC ")
        assertEquals(setOf("aaa", "bbb", "ccc"), parsed)
        assertTrue(parseFingerprintSet(null).isEmpty())
    }

    @Test
    fun matchesBySha256MembershipInJoinedSet() {
        val installed = CertFingerprint.of("AAAA", null)
        val candidate = candidate(sigSha256 = "bbbb AAAA cccc")
        assertTrue(candidate.matchesInstalled(installed))
    }

    @Test
    fun matchesByMd5WhenSha256Unknown() {
        val installed = CertFingerprint.of(null, "d41d8cd98f00b204e9800998ecf8427e")
        val candidate = candidate(sigMd5 = "deadbeef d41d8cd98f00b204e9800998ecf8427e")
        assertTrue(candidate.matchesInstalled(installed))
    }

    @Test
    fun doesNotMatchDifferentSigningKeyEvenIfVersionNewer() {
        val installed = CertFingerprint.of("1111", "2222")
        val candidate = candidate(versionCode = 99, sigSha256 = "3333", sigMd5 = "4444")
        assertFalse(candidate.matchesInstalled(installed))
        assertTrue(candidate.isNewerThan(1))
    }

    @Test
    fun unknownInstalledFingerprintNeverMatches() {
        assertFalse(candidate(sigSha256 = "aaaa").matchesInstalled(null))
        assertFalse(candidate(sigSha256 = "aaaa").matchesInstalled(CertFingerprint.of(null, null)))
    }

    @Test
    fun newerComparisonRequiresBothVersionCodes() {
        assertTrue(candidate(versionCode = 5).isNewerThan(4))
        assertFalse(candidate(versionCode = 4).isNewerThan(4))
        assertFalse(candidate(versionCode = null).isNewerThan(4))
        assertFalse(candidate(versionCode = 5).isNewerThan(null))
    }

    private fun candidate(
        versionCode: Long? = 1,
        sigSha256: String? = null,
        sigMd5: String? = null
    ): AppCandidate = AppCandidate(
        id = 1,
        appSlug = "app",
        packageName = "com.app",
        versionCode = versionCode,
        versionName = "1.0",
        source = null,
        apkUrl = "https://example/app.apk",
        archiveEntry = null,
        size = 10,
        sha256 = null,
        sigSha256 = sigSha256,
        sigMd5 = sigMd5,
        minSdk = 24,
        isPrimary = true
    )
}
