/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

sealed interface DownloadError {
    data class Network(val cause: Throwable) : DownloadError
    data class Http(val code: Int) : DownloadError
    data class HashMismatch(val expected: String, val actual: String) : DownloadError
    data class Archive(val entry: String) : DownloadError
    data class InsufficientStorage(
        val requiredBytes: Long,
        val availableBytes: Long
    ) : DownloadError
    data class MirrorExhausted(val tried: Int) : DownloadError
}
