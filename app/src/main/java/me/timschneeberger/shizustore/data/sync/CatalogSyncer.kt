/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.sync

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.timschneeberger.shizustore.data.api.ApiError
import me.timschneeberger.shizustore.data.api.ApiResult
import me.timschneeberger.shizustore.data.api.AppsQuery
import me.timschneeberger.shizustore.data.api.EtagResult
import me.timschneeberger.shizustore.data.api.ShizuApi
import me.timschneeberger.shizustore.data.helper.SyncStatusStore
import me.timschneeberger.shizustore.data.repository.UpdateStateRepository
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.CategoryDao
import me.timschneeberger.shizustore.data.room.dao.SyncStateDao
import me.timschneeberger.shizustore.data.room.entity.SyncStateEntity

/** Failure classes the sync worker and UI can act on. */
enum class CatalogSyncFailure {
    NETWORK,
    HTTP,
    PARSE,
    RATE_LIMITED,
    NOT_CONFIGURED
}

sealed interface CatalogSyncOutcome {
    data class Success(val added: Int, val updated: Int, val removed: Int) : CatalogSyncOutcome

    data object AlreadyRunning : CatalogSyncOutcome

    data class Failed(
        val failure: CatalogSyncFailure,
        val message: String? = null
    ) : CatalogSyncOutcome
}

/** Runs single-flight; a second caller gets [CatalogSyncOutcome.AlreadyRunning]. */
@Singleton
class CatalogSyncer @Inject constructor(
    private val api: ShizuApi,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val syncStateDao: SyncStateDao,
    private val updateStateRepository: UpdateStateRepository,
    private val syncStatus: SyncStatusStore
) {
    private val mutex = Mutex()

    suspend fun sync(): CatalogSyncOutcome {
        if (!mutex.tryLock()) return CatalogSyncOutcome.AlreadyRunning
        return try {
            run().also { outcome ->
                syncStatus.set((outcome as? CatalogSyncOutcome.Failed)?.failure)
            }
        } finally {
            mutex.unlock()
        }
    }

    /**
     * Drops the cached catalog and the sync cursor. Taking the mutex keeps a
     * running sync from writing rows back after the wipe; the next sync then
     * bootstraps instead of replaying deltas that belong to the previous server.
     */
    suspend fun clearCatalog() {
        mutex.withLock {
            syncStateDao.clear()
            categoryDao.clear()
            // app_download rows cascade with their app.
            appDao.clear()
        }
    }

    private suspend fun run(): CatalogSyncOutcome {
        val state = syncStateDao.get()
        val counts = if (state?.cursor.isNullOrBlank()) {
            when (val result = bootstrap()) {
                is StepResult.Failed -> return result.outcome
                is StepResult.Ok -> result.counts
            }
        } else {
            when (val result = incremental(state.cursor)) {
                is StepResult.Failed -> return result.outcome
                is StepResult.Ok -> result.counts
            }
        }

        val categoriesEtag = refreshCategories(state?.categoriesEtag)
        val meta = when (val result = api.meta()) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return result.error.toOutcome()
        }

        syncStateDao.upsert(
            SyncStateEntity(
                cursor = meta.generatedAt,
                categoriesEtag = categoriesEtag,
                listCommit = meta.listCommit,
                syncedAt = System.currentTimeMillis(),
                useInstallCountsForPopularity = meta.useInstallCountsForPopularity
            )
        )
        updateStateRepository.recomputeAll()
        return CatalogSyncOutcome.Success(counts.added, counts.updated, counts.removed)
    }

    /** First run: page the whole catalog, then drop rows the server no longer has. */
    private suspend fun bootstrap(): StepResult {
        val seen = mutableListOf<String>()
        var page = 1
        while (page <= MAX_BOOTSTRAP_PAGES) {
            val result = api.apps(
                AppsQuery(
                    page = page,
                    pageSize = BOOTSTRAP_PAGE_SIZE,
                    sort = SORT_NAME,
                    order = ORDER_ASC
                )
            )
            val dto = when (result) {
                is ApiResult.Success -> result.value
                is ApiResult.Failure -> return StepResult.Failed(result.error.toOutcome())
            }

            val now = System.currentTimeMillis()
            val entities = dto.items.map { it.toEntity(now) }
            if (entities.isNotEmpty()) {
                appDao.upsertSummaries(entities)
                seen += entities.map { it.slug }
            }

            if (entities.isEmpty() || seen.size >= dto.total) break
            page++
        }

        val stale = appDao.allSlugs().filterNot { it in seen }
        if (stale.isNotEmpty()) appDao.deleteBySlugs(stale)

        return StepResult.Ok(Counts(added = seen.size))
    }

    private suspend fun incremental(since: String): StepResult {
        val changes = when (val result = api.changes(since)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return StepResult.Failed(result.error.toOutcome())
        }

        val now = System.currentTimeMillis()
        val upserts = (changes.added + changes.updated).map { it.toEntity(now) }
        if (upserts.isNotEmpty()) appDao.upsertSummaries(upserts)
        if (changes.removed.isNotEmpty()) appDao.deleteBySlugs(changes.removed.map { it.slug })
        // Install-count deltas land last and bypass the summary path: they
        // touch one column, so a hot counter never marks rows for refetch.
        if (changes.installsUpdated.isNotEmpty()) appDao.setInstallCounts(changes.installsUpdated)

        return StepResult.Ok(
            Counts(
                added = changes.added.size,
                updated = changes.updated.size,
                removed = changes.removed.size
            )
        )
    }

    /** Category failures are non-fatal: the catalog stays usable with the previous tree. */
    private suspend fun refreshCategories(etag: String?): String? =
        when (val result = api.categories(etag)) {
            is ApiResult.Failure -> etag
            is ApiResult.Success -> when (val value = result.value) {
                is EtagResult.Data -> {
                    categoryDao.replaceAll(value.value.toEntities())
                    value.etag ?: etag
                }
                EtagResult.NotModified, EtagResult.NotFound -> etag
            }
        }

    private sealed interface StepResult {
        data class Ok(val counts: Counts) : StepResult

        data class Failed(val outcome: CatalogSyncOutcome) : StepResult
    }

    private data class Counts(val added: Int = 0, val updated: Int = 0, val removed: Int = 0)

    private fun ApiError.toOutcome(): CatalogSyncOutcome.Failed = CatalogSyncOutcome.Failed(
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
        const val BOOTSTRAP_PAGE_SIZE = 200
        const val MAX_BOOTSTRAP_PAGES = 50
        const val SORT_NAME = "name"
        const val ORDER_ASC = "asc"
    }
}
