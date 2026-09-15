/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

/** Query for `GET /v1/apps`; null fields are omitted (server defaults apply). */
data class AppsQuery(
    val category: String? = null,
    val q: String? = null,
    val license: String? = null,
    val listing: String? = null,
    val availability: String? = null,
    val type: String? = null,
    val recommended: Boolean? = null,
    val page: Int = 1,
    val pageSize: Int = 50,
    val sort: String? = null,
    val order: String? = null
)

/** Thin typed client over the public version 1 API. */
interface ShizuApi {
    suspend fun apps(query: AppsQuery = AppsQuery()): ApiResult<PagedAppsDto>

    /** 404 -> [EtagResult.NotFound]; 304 -> [EtagResult.NotModified]. */
    suspend fun app(slug: String, etag: String? = null): ApiResult<EtagResult<AppDetailDto>>

    /** 304 -> [EtagResult.NotModified]. */
    suspend fun categories(etag: String? = null): ApiResult<EtagResult<List<CategoryNodeDto>>>

    suspend fun changes(since: String): ApiResult<ChangesDto>

    suspend fun meta(): ApiResult<MetaDto>

    suspend fun health(): ApiResult<HealthDto>

    /**
     * Records one successful install of [slug] (`POST
     * /v1/apps/{slug}/installs`). 404 (unknown slug) surfaces as
     * [ApiError.Http]; callers treat every outcome as best-effort.
     */
    suspend fun reportInstall(slug: String): ApiResult<InstallRecordedDto>
}
