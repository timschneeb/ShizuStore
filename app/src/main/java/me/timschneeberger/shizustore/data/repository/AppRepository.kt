/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import androidx.paging.PagingSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.data.model.DetailedApp
import me.timschneeberger.shizustore.data.room.AppListQueryBuilder
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.AppDownloadDao
import me.timschneeberger.shizustore.data.room.dao.CategoryDao
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.CategoryEntity

/** Read model over the synced catalog. All queries are Room backed; the network sync lives elsewhere. */
@Singleton
class AppRepository @Inject constructor(
    private val appDao: AppDao,
    private val appDownloadDao: AppDownloadDao,
    private val categoryDao: CategoryDao
) {
    /** The shared list: one parameterized query for every [AppListArgs] combination. */
    fun pagedApps(args: AppListArgs): PagingSource<Int, AppEntity> =
        appDao.pagedFiltered(AppListQueryBuilder.build(args))

    fun pagedInstalled(): PagingSource<Int, AppEntity> = appDao.pagedInstalled()

    fun pagedUpdatable(): PagingSource<Int, AppEntity> = appDao.pagedUpdatable()

    fun observeUpdatableCount(): Flow<Int> = appDao.observeUpdatableCount()

    fun observeRecentlyAdded(limit: Int = CAROUSEL_LIMIT): Flow<List<AppEntity>> =
        appDao.observeRecentlyAdded(limit)

    fun observeRecommendedPool(): Flow<List<AppEntity>> = appDao.observeRecommendedPool()

    fun observeByAuthor(
        authorKey: String,
        excludeSlug: String,
        limit: Int = CAROUSEL_LIMIT
    ): Flow<List<AppEntity>> = appDao.observeByAuthor(authorKey, excludeSlug, limit)

    fun observeRecentlyUpdated(limit: Int = CAROUSEL_LIMIT): Flow<List<AppEntity>> =
        appDao.observeRecentlyUpdated(limit)

    fun observeRandomPool(): Flow<List<AppEntity>> = appDao.observeAll()

    fun pagedFavourites(): PagingSource<Int, AppEntity> = appDao.pagedFavourites()

    fun observeBlacklisted(): Flow<List<AppEntity>> = appDao.observeBlacklisted()

    fun observeIgnoredUpdates(): Flow<List<AppEntity>> = appDao.observeIgnoredUpdates()

    fun observe(slug: String): Flow<AppEntity?> = appDao.observe(slug)

    fun observeByPackage(packageName: String): Flow<List<AppEntity>> =
        appDao.observeByPackage(packageName)

    fun observeDetail(slug: String): Flow<DetailedApp?> = combine(
        appDao.observe(slug),
        appDownloadDao.observeForApp(slug)
    ) { app, downloads ->
        app?.let {
            DetailedApp(it, downloads.map { entity -> AppCandidate.from(entity, it.packageName) })
        }
    }

    suspend fun get(slug: String): AppEntity? = appDao.get(slug)

    suspend fun getByPackage(packageName: String): AppEntity? = appDao.getByPackage(packageName)

    suspend fun getAll(): List<AppEntity> = appDao.getAll()

    /** Updatable rows with a real package name, the only ones an install can target. */
    suspend fun updatableApps(): List<AppEntity> =
        appDao.getAll().filter { it.updateAvailable && it.packageName != null }

    suspend fun detail(slug: String): DetailedApp? {
        val app = appDao.get(slug) ?: return null
        val candidates = appDownloadDao.forApp(slug).map { AppCandidate.from(it, app.packageName) }
        return DetailedApp(app, candidates)
    }

    suspend fun candidate(id: Long): AppCandidate? = appDownloadDao.getById(id)?.let { entity ->
        val packageName = appDao.get(entity.appSlug)?.packageName
        AppCandidate.from(entity, packageName)
    }

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun categories(): List<CategoryEntity> = categoryDao.getAll()

    companion object {
        const val CAROUSEL_LIMIT = 20
    }
}
