/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * OkHttp implementation of [ShizuApi]. Reuses the Hilt [OkHttpClient] (proxy,
 * cache, timeouts) and serializes with [ShizuJson]; requests are funneled
 * through [RequestThrottle].
 */
@Singleton
class OkHttpShizuApi @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrlProvider: BaseUrlProvider,
    private val throttle: RequestThrottle
) : ShizuApi {

    override suspend fun apps(query: AppsQuery): ApiResult<PagedAppsDto> =
        decode(execute("/v1/apps", query.toParams())) { body ->
            json.decodeFromString(PagedAppsDto.serializer(), body)
        }

    override suspend fun app(slug: String, etag: String?): ApiResult<EtagResult<AppDetailDto>> =
        when (val result = execute("/v1/apps/$slug", emptyList(), etag)) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> when (val raw = result.value) {
                is RawResponse.Ok -> try {
                    val detail = json.decodeFromString(AppDetailDto.serializer(), raw.body)
                    ApiResult.Success(EtagResult.Data(detail, raw.etag))
                } catch (e: SerializationException) {
                    ApiResult.Failure(ApiError.Parse(e.message, e))
                }
                RawResponse.NotModified -> ApiResult.Success(EtagResult.NotModified)
                RawResponse.NotFound -> ApiResult.Success(EtagResult.NotFound)
            }
        }

    override suspend fun categories(etag: String?): ApiResult<EtagResult<List<CategoryNodeDto>>> =
        when (val result = execute("/v1/categories", emptyList(), etag)) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> when (val raw = result.value) {
                is RawResponse.Ok -> try {
                    val nodes = json.decodeFromString(categoryListSerializer, raw.body)
                    ApiResult.Success(EtagResult.Data(nodes, raw.etag))
                } catch (e: SerializationException) {
                    ApiResult.Failure(ApiError.Parse(e.message, e))
                }
                RawResponse.NotModified -> ApiResult.Success(EtagResult.NotModified)
                RawResponse.NotFound -> ApiResult.Success(EtagResult.NotFound)
            }
        }

    override suspend fun changes(since: String): ApiResult<ChangesDto> =
        decode(execute("/v1/changes", listOf("since" to since))) { body ->
            json.decodeFromString(ChangesDto.serializer(), body)
        }

    override suspend fun meta(): ApiResult<MetaDto> = decode(execute("/v1/meta")) { body ->
        json.decodeFromString(MetaDto.serializer(), body)
    }

    override suspend fun health(): ApiResult<HealthDto> = decode(execute("/healthz")) { body ->
        json.decodeFromString(HealthDto.serializer(), body)
    }

    override suspend fun reportInstall(slug: String): ApiResult<InstallRecordedDto> =
        decode(post("/v1/apps/$slug/installs")) { body ->
            json.decodeFromString(InstallRecordedDto.serializer(), body)
        }

    private suspend fun execute(
        path: String,
        params: List<Pair<String, String>> = emptyList(),
        etag: String? = null
    ): ApiResult<RawResponse> {
        val base = baseUrlProvider.current()
        val url = base.takeIf { it.isNotBlank() }?.let { buildUrl(it, path, params) }
            ?: return ApiResult.Failure(ApiError.NotConfigured)

        // The catalog is dynamic and synced into Room, so never let the shared
        // OkHttp cache serve an API response without hitting the network: a
        // cached /v1/changes would make a manual refresh a no-op.
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/json")
            .apply { if (!etag.isNullOrBlank()) header("If-None-Match", etag) }
            .build()

        return perform(request)
    }

    private suspend fun post(path: String): ApiResult<RawResponse> {
        val base = baseUrlProvider.current()
        val url = base.takeIf { it.isNotBlank() }?.let { buildUrl(it, path, emptyList()) }
            ?: return ApiResult.Failure(ApiError.NotConfigured)

        // Reports are fire-and-forget writes: never cache them, and the empty
        // body keeps the request small (the slug rides in the path).
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/json")
            .post(ByteArray(0).toRequestBody())
            .build()

        return perform(request)
    }

    private suspend fun perform(request: Request): ApiResult<RawResponse> =
        withContext(Dispatchers.IO) {
            try {
                throttle.acquire()
                client.newCall(request).execute().use { response ->
                    when {
                        response.code == HTTP_NOT_MODIFIED -> {
                            throttle.reset()
                            ApiResult.Success(RawResponse.NotModified)
                        }
                        response.code == HTTP_NOT_FOUND -> {
                            throttle.reset()
                            ApiResult.Success(RawResponse.NotFound)
                        }
                        response.code == HTTP_TOO_MANY_REQUESTS -> {
                            throttle.penalize()
                            ApiResult.Failure(ApiError.RateLimited(response.retryAfterSeconds()))
                        }
                        response.isSuccessful -> {
                            throttle.reset()
                            ApiResult.Success(
                                RawResponse.Ok(
                                    body = response.body.string(),
                                    etag = response.header("ETag")
                                )
                            )
                        }
                        else -> {
                            throttle.reset()
                            ApiResult.Failure(
                                ApiError.Http(
                                    response.code,
                                    response.body.string().take(ERROR_BODY_LIMIT)
                                )
                            )
                        }
                    }
                }
            } catch (e: IOException) {
                ApiResult.Failure(ApiError.Network(e.message, e))
            }
        }

    private inline fun <T> decode(
        result: ApiResult<RawResponse>,
        parser: (String) -> T
    ): ApiResult<T> = when (result) {
        is ApiResult.Failure -> result
        is ApiResult.Success -> when (val raw = result.value) {
            is RawResponse.Ok -> try {
                ApiResult.Success(parser(raw.body))
            } catch (e: SerializationException) {
                ApiResult.Failure(ApiError.Parse(e.message, e))
            }
            RawResponse.NotModified -> ApiResult.Failure(
                ApiError.Http(HTTP_NOT_MODIFIED, "Unexpected 304")
            )
            RawResponse.NotFound -> ApiResult.Failure(ApiError.Http(HTTP_NOT_FOUND, "Not found"))
        }
    }

    private fun buildUrl(base: String, path: String, params: List<Pair<String, String>>): HttpUrl? {
        val parsed = base.toHttpUrlOrNull() ?: return null
        return parsed.newBuilder().apply {
            path.split('/').filter { it.isNotEmpty() }.forEach { addPathSegment(it) }
            params.forEach { (name, value) -> addQueryParameter(name, value) }
        }.build()
    }

    private fun AppsQuery.toParams(): List<Pair<String, String>> = buildList {
        category?.let { add("category" to it) }
        q?.let { add("q" to it) }
        license?.let { add("license" to it) }
        listing?.let { add("listing" to it) }
        availability?.let { add("availability" to it) }
        type?.let { add("type" to it) }
        recommended?.let { add("recommended" to it.toString()) }
        add("page" to page.toString())
        add("pageSize" to pageSize.toString())
        sort?.let { add("sort" to it) }
        order?.let { add("order" to it) }
    }

    private fun Response.retryAfterSeconds(): Long? = header("Retry-After")?.trim()?.toLongOrNull()

    private sealed interface RawResponse {
        data class Ok(val body: String, val etag: String?) : RawResponse

        data object NotModified : RawResponse

        data object NotFound : RawResponse
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val ERROR_BODY_LIMIT = 512

        val categoryListSerializer = ListSerializer(CategoryNodeDto.serializer())
    }
}
