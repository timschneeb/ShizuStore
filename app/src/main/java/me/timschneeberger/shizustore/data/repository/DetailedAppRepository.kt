/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import java.util.Collections
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.api.ApiError
import me.timschneeberger.shizustore.data.api.ApiResult
import me.timschneeberger.shizustore.data.api.AppDetailDto
import me.timschneeberger.shizustore.data.api.EtagResult
import me.timschneeberger.shizustore.data.api.ShizuApi
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.AppDownloadDao
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure
import me.timschneeberger.shizustore.data.sync.applyDetail
import me.timschneeberger.shizustore.data.sync.toEntity

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
 * Summaries always exist first (the catalog sync writes them), so an unknown
 * slug is [DetailedAppResult.NotFound] rather than created.
 */
@Singleton
class DetailedAppRepository @Inject constructor(
    private val api: ShizuApi,
    private val appDao: AppDao,
    private val appDownloadDao: AppDownloadDao,
    private val updateStateRepository: UpdateStateRepository
) {
    /** Server-only, never in Room: a small LRU keeps the README until its subscreen opens. */
    private val fullDescriptions = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>) =
                size > MAX_CACHED_DESCRIPTIONS
        }
    )

    fun fullDescription(slug: String): String? = fullDescriptions[slug]

    /** Same process-lifetime LRU as [fullDescriptions]; the changelog screen reads one app at a time. */
    private val changelogs = Collections.synchronizedMap(
        object : LinkedHashMap<String, String>(CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>) =
                size > MAX_CACHED_DESCRIPTIONS
        }
    )

    fun changelog(slug: String): String? = changelogs[slug]

    /** Same process-lifetime LRU as [fullDescriptions]; the details gallery reads one app at a time. */
    private val screenshots = Collections.synchronizedMap(
        object : LinkedHashMap<String, List<String>>(CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>) =
                size > MAX_CACHED_DESCRIPTIONS
        }
    )

    fun screenshots(slug: String): List<String>? = screenshots[slug]

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

    private suspend fun persist(slug: String, detail: AppDetailDto): DetailedAppResult {
        val existing = appDao.get(slug) ?: return DetailedAppResult.NotFound
        val updated = existing.applyDetail(detail, System.currentTimeMillis())
        appDao.upsert(updated)

        detail.fullDescription?.takeIf { it.isNotBlank() }?.let { fullDescriptions[slug] = it }
        detail.changelog?.takeIf { it.isNotBlank() }?.let { changelogs[slug] = it }
        detail.screenshots.takeIf { it.isNotEmpty() }?.let { screenshots[slug] = it }

        val downloads = detail.downloads.map { it.toEntity(slug) }
        appDownloadDao.replaceForApp(slug, downloads)
        // Replaced candidate ids invalidate a previously pinned updateCandidateId.
        updateStateRepository.recompute(updated.packageName ?: slug)
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

    private companion object {
        const val CACHE_SIZE = 4
        const val MAX_CACHED_DESCRIPTIONS = 3
    }
}
