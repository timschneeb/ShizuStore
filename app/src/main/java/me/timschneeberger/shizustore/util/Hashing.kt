/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import java.io.File
import java.security.MessageDigest

/** Lowercase hex digest helpers, independent of the F-Droid index hashing code. */
object Hashing {

    fun sha256Hex(bytes: ByteArray): String = digestHex("SHA-256", bytes)

    fun md5Hex(bytes: ByteArray): String = digestHex("MD5", bytes)

    fun sha256Hex(file: File): String = digestHex("SHA-256", file)

    fun md5Hex(file: File): String = digestHex("MD5", file)

    private fun digestHex(algorithm: String, bytes: ByteArray): String =
        MessageDigest.getInstance(algorithm).digest(bytes).toHex()

    private fun digestHex(algorithm: String, file: File): String {
        val digest = MessageDigest.getInstance(algorithm)
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }
}
