/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.timschneeberger.shizustore.data.api.ApiError
import me.timschneeberger.shizustore.data.api.ApiResult
import me.timschneeberger.shizustore.data.api.AppsQuery
import me.timschneeberger.shizustore.data.api.EtagResult
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.api.ShizuApi
import me.timschneeberger.shizustore.data.helper.SyncStatusStore
import me.timschneeberger.shizustore.data.repository.UpdateStateRepository
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.CategoryDao
import me.timschneeberger.shizustore.data.room.dao.SyncStateDao
import me.timschneeberger.shizustore.data.room.entity.SyncStateEntity
import me.timschneeberger.shizustore.util.CommonUtil
import me.timschneeberger.shizustore.util.Preferences

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
    @ApplicationContext private val context: Context,
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
            clearCatalogLocked()
        }
    }

    private suspend fun clearCatalogLocked() {
        syncStateDao.clear()
        categoryDao.clear()
        // app_download rows cascade with their app.
        appDao.clear()
    }

    /**
     * Remote purge: record the applied timestamp first, then drop the catalog
     * tables. Favourites and the blocklist are user data and never touched; the
     * marker lives in DataStore so it survives this wipe.
     */
    private suspend fun purgeCatalogLocked(requestedAtMillis: Long) {
        Preferences.putLong(
            context,
            Preferences.PREFERENCE_LAST_CATALOG_PURGE_AT,
            requestedAtMillis
        )
        clearCatalogLocked()
    }

    /**
     * A listing change invalidates the cursor: deltas never carry rows from a
     * listing that was not in the previous set, so the next sync has to
     * bootstrap. Opting out also drops the closed rows immediately.
     */
    suspend fun onShowClosedSourceChanged(enabled: Boolean) {
        mutex.withLock {
            if (!enabled) {
                appDao.deleteByListing(Listing.CLOSED_SOURCE)
                updateStateRepository.recomputeAll()
            }
            syncStateDao.clearCursor()
        }
    }

    private suspend fun run(): CatalogSyncOutcome {
        val state = syncStateDao.get()
        val listing = listingParam()
        var purged = false
        val counts = if (state?.cursor.isNullOrBlank()) {
            when (val result = bootstrap(listing)) {
                is StepResult.Failed -> return result.outcome
                is StepResult.Ok -> result.counts
                is StepResult.Purge -> error("bootstrap never requests a purge")
            }
        } else {
            when (val result = incremental(state.cursor, listing)) {
                is StepResult.Failed -> return result.outcome
                is StepResult.Ok -> result.counts
                is StepResult.Purge -> {
                    purgeCatalogLocked(result.requestedAtMillis)
                    purged = true
                    when (val fresh = bootstrap(listing)) {
                        is StepResult.Failed -> return fresh.outcome
                        is StepResult.Ok -> fresh.counts
                        is StepResult.Purge -> error("bootstrap never requests a purge")
                    }
                }
            }
        }

        // A purge emptied the category table; reusing the old ETag could 304
        // and leave it empty.
        val categoriesEtag =
            refreshCategories(if (purged) null else state?.categoriesEtag, listing)
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
    private suspend fun bootstrap(listing: String): StepResult {
        val seen = mutableListOf<String>()
        var page = 1
        while (page <= MAX_BOOTSTRAP_PAGES) {
            val result = api.apps(
                AppsQuery(
                    page = page,
                    pageSize = BOOTSTRAP_PAGE_SIZE,
                    sort = SORT_NAME,
                    order = ORDER_ASC,
                    listing = listing
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

    private suspend fun incremental(since: String, listing: String): StepResult {
        val changes = when (val result = api.changes(since, listing)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> return StepResult.Failed(result.error.toOutcome())
        }

        // A newer operator purge wins over the deltas: they describe a catalog
        // the client is about to drop, and a bootstrap is the only complete refill.
        val purgeAt = CommonUtil.parseIsoUtcMillis(changes.catalogPurgeRequestedAt)
        if (purgeAt != null && purgeAt > lastPurgeApplied()) {
            return StepResult.Purge(purgeAt)
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
    private suspend fun refreshCategories(etag: String?, listing: String): String? =
        when (val result = api.categories(etag, listing)) {
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

        /** The server requested a purge newer than the one applied locally. */
        data class Purge(val requestedAtMillis: Long) : StepResult
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

    private suspend fun lastPurgeApplied(): Long =
        Preferences.readLong(context, Preferences.PREFERENCE_LAST_CATALOG_PURGE_AT, 0L)

    private suspend fun listingParam(): String =
        if (Preferences.readBoolean(context, Preferences.PREFERENCE_SHOW_CLOSED_SOURCE, false)) {
            LISTING_BOTH
        } else {
            LISTING_MAIN
        }

    private companion object {
        const val BOOTSTRAP_PAGE_SIZE = 200
        const val MAX_BOOTSTRAP_PAGES = 50
        const val SORT_NAME = "name"
        const val ORDER_ASC = "asc"
        const val LISTING_MAIN = "main"
        const val LISTING_BOTH = "main,closed_source"
    }
}
