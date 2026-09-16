/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Droid-ify (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.network

import java.io.File
import okhttp3.Headers

interface Downloader {
    suspend fun downloadToFile(
        url: String,
        target: File,
        resume: Boolean = false,
        headers: Headers.Builder.() -> Unit = {},
        block: ProgressListener? = null
    ): NetworkResponse
}

typealias ProgressListener = suspend (bytesReceived: DataSize, contentLength: DataSize?) -> Unit
