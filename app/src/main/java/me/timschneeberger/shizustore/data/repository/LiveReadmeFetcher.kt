/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches the README from its raw forge URL so "More about this app" shows the
 * live file instead of the (possibly old) server snapshot. Fail-soft: any
 * error returns null and the caller keeps the snapshot.
 */
@Singleton
class LiveReadmeFetcher @Inject constructor(
    private val http: OkHttpClient
) {
    suspend fun fetch(url: String): String? = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://") && !url.startsWith("http://")) return@withContext null
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()
        try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.peekBody(MAX_README_BYTES).string().takeIf { it.isNotBlank() }
            }
        } catch (_: IOException) {
            null
        } catch (_: IllegalArgumentException) {
            // OkHttp rejects malformed URLs; the snapshot remains the fallback.
            null
        }
    }

    private companion object {
        const val MAX_README_BYTES = 2L * 1024 * 1024
    }
}
