/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.network

import coil3.annotation.ExperimentalCoilApi
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options

/**
 * Coil caches 404s and then serves them from disk forever, so only 2xx responses
 * are cached.
 */
@OptIn(ExperimentalCoilApi::class)
class SuccessOnlyImageCacheStrategy : CacheStrategy {
    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options
    ): CacheStrategy.ReadResult = if (isCacheableResponse(cacheResponse.code)) {
        CacheStrategy.ReadResult(cacheResponse)
    } else {
        CacheStrategy.ReadResult(networkRequest)
    }

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options
    ): CacheStrategy.WriteResult = if (isCacheableResponse(networkResponse.code)) {
        CacheStrategy.WriteResult(networkResponse)
    } else {
        CacheStrategy.WriteResult.DISABLED
    }
}

/** Only 2xx bodies are safe to replay; 304 is revalidated by the HTTP client. */
internal fun isCacheableResponse(code: Int): Boolean = code in 200 until 300
