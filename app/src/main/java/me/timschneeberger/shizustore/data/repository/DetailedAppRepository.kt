/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.api.ApiError
import me.timschneeberger.shizustore.data.api.ApiResult
import me.timschneeberger.shizustore.data.api.EtagResult
import me.timschneeberger.shizustore.data.api.ShizuApi
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.AppDownloadDao
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure
import me.timschneeberger.shizustore.data.sync.applyDetail
import me.timschneeberger.shizustore.data.sync.toEntity

/** Outcome of fetching one app's detail and install candidates. */
sealed interface DetailedAppResult {
    data class Success(
        val app: AppEntity,
        val downloads: List<AppDownloadEntity>
    ) : DetailedAppResult

    data object NotFound : DetailedAppResult

    data class Failed(
        val failure: CatalogSyncFailure,
        val message: String? = null
    ) : DetailedAppResult
}

/**
 * Fetches `/v1/apps/{slug}` and persists its detail columns and candidate list.
 * Summaries always exist first (the catalog sync writes them), so an unknown
 * slug is treated as [DetailedAppResult.NotFound] rather than created.
 */
@Singleton
class DetailedAppRepository @Inject constructor(
    private val api: ShizuApi,
    private val appDao: AppDao,
    private val appDownloadDao: AppDownloadDao
) {
    suspend fun fetchAndPersist(slug: String): DetailedAppResult =
        when (val result = api.app(slug)) {
            is ApiResult.Failure -> result.error.toFailure()
            is ApiResult.Success -> when (val value = result.value) {
                EtagResult.NotFound -> DetailedAppResult.NotFound
                EtagResult.NotModified -> cached(slug)
                is EtagResult.Data -> persist(slug, value.value)
            }
        }

    private suspend fun cached(slug: String): DetailedAppResult {
        val app = appDao.get(slug) ?: return DetailedAppResult.NotFound
        return DetailedAppResult.Success(app, appDownloadDao.forApp(slug))
    }

    private suspend fun persist(
        slug: String,
        detail: me.timschneeberger.shizustore.data.api.AppDetailDto
    ): DetailedAppResult {
        val existing = appDao.get(slug) ?: return DetailedAppResult.NotFound
        val updated = existing.applyDetail(detail, System.currentTimeMillis())
        appDao.upsert(updated)

        val downloads = detail.downloads.map { it.toEntity(slug) }
        appDownloadDao.replaceForApp(slug, downloads)
        return DetailedAppResult.Success(updated, downloads)
    }

    private fun ApiError.toFailure(): DetailedAppResult.Failed = DetailedAppResult.Failed(
        failure = when (this) {
            is ApiError.Network -> CatalogSyncFailure.NETWORK
            is ApiError.Http -> CatalogSyncFailure.HTTP
            is ApiError.Parse -> CatalogSyncFailure.PARSE
            is ApiError.RateLimited -> CatalogSyncFailure.RATE_LIMITED
            ApiError.NotConfigured -> CatalogSyncFailure.NOT_CONFIGURED
        },
        message = message
    )
}
