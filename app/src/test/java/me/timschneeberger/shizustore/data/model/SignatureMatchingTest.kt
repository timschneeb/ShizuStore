/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignatureMatchingTest {
    private val shaA = "a".repeat(64)
    private val shaB = "b".repeat(64)
    private val md5A = "c".repeat(32)

    @Test
    fun anyInstalledIdentityMatches() {
        val installed = listOf(
            CertFingerprint(sha256 = setOf(shaA), md5 = setOf(md5A)),
            CertFingerprint(sha256 = setOf(shaB), md5 = emptySet())
        )
        assertTrue(signaturesMatch(installed, shaB, null))
    }

    @Test
    fun md5OnlyMatchIsAccepted() {
        val installed = CertFingerprint(sha256 = emptySet(), md5 = setOf(md5A))
        assertTrue(fingerprintMatches(installed, sigSha256 = null, sigMd5 = md5A))
    }

    @Test
    fun joinedSetRotationMatchesByMembership() {
        val installed = CertFingerprint(sha256 = setOf(shaB), md5 = emptySet())
        assertTrue(fingerprintMatches(installed, sigSha256 = "$shaA $shaB", sigMd5 = null))
    }

    @Test
    fun differentKeyDoesNotMatch() {
        val installed = CertFingerprint(sha256 = setOf(shaA), md5 = emptySet())
        assertFalse(fingerprintMatches(installed, sigSha256 = shaB, sigMd5 = null))
        assertFalse(signaturesMatch(listOf(installed), shaB, null))
    }

    @Test
    fun unknownInstalledNeverMatches() {
        val unknown = CertFingerprint(sha256 = emptySet(), md5 = emptySet())
        assertFalse(fingerprintMatches(unknown, sigSha256 = shaA, sigMd5 = md5A))
    }
}
