/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

/** Result of an API call: either a decoded value or a typed [ApiError]. */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/**
 * Failure modes the client can act on. [ApiError.RateLimited] carries the
 * server `Retry-After` when present even though the fixed window usually omits
 * it.
 */
sealed interface ApiError {
    val message: String?

    data class Network(override val message: String?, val cause: Throwable? = null) : ApiError

    data class Http(val code: Int, override val message: String?) : ApiError

    data class Parse(override val message: String?, val cause: Throwable? = null) : ApiError

    data class RateLimited(val retryAfterSeconds: Long?) : ApiError {
        override val message: String = "Rate limited"
    }

    data object NotConfigured : ApiError {
        override val message: String = "Server base URL is not configured"
    }
}

/** ETag-aware outcome for endpoints that expose `ETag`/`If-None-Match`. */
sealed interface EtagResult<out T> {
    data class Data<T>(val value: T, val etag: String?) : EtagResult<T>

    data object NotModified : EtagResult<Nothing>

    data object NotFound : EtagResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}
