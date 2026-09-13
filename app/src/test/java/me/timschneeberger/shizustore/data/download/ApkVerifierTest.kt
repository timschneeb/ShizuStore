/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.download

import java.io.File
import me.timschneeberger.shizustore.util.Hashing
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ApkVerifierTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun file(bytes: ByteArray): File = temp.newFile().apply { writeBytes(bytes) }

    @Test
    fun sha256MatchPasses() {
        val bytes = "hello".toByteArray()
        assertNull(ApkVerifier.verify(file(bytes), Hashing.sha256Hex(bytes)))
    }

    @Test
    fun md5HashTypeUsesMd5() {
        val bytes = "hello".toByteArray()
        assertNull(ApkVerifier.verify(file(bytes), Hashing.md5Hex(bytes), hashType = "md5"))
    }

    @Test
    fun mismatchIsReported() {
        assertNotNull(ApkVerifier.verify(file("hello".toByteArray()), "0".repeat(64)))
    }

    @Test
    fun blankHashFails() {
        assertNotNull(ApkVerifier.verify(file("hello".toByteArray()), ""))
    }
}
