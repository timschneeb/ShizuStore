/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.download

import java.io.File
import me.timschneeberger.shizustore.data.model.DownloadError
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.util.Hashing

object ApkVerifier {

    /** Verifies a downloaded artifact against the server hash. The server uses sha256 today. */
    fun verify(
        file: File,
        expectedHash: String,
        hashType: String = Download.HASH_SHA256
    ): DownloadError.HashMismatch? {
        if (expectedHash.isBlank()) {
            return DownloadError.HashMismatch(expected = expectedHash, actual = "<no hash>")
        }

        val actual = runCatching {
            if (hashType.equals("md5", ignoreCase = true)) {
                Hashing.md5Hex(file)
            } else {
                Hashing.sha256Hex(file)
            }
        }.getOrElse {
            return DownloadError.HashMismatch(expected = expectedHash, actual = "<unreadable>")
        }

        return if (actual.equals(expectedHash, ignoreCase = true)) {
            null
        } else {
            DownloadError.HashMismatch(expected = expectedHash, actual = actual)
        }
    }
}
