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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.data.model.DetailedApp
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.model.matchInstalled
import me.timschneeberger.shizustore.data.room.AppListQueryBuilder
import me.timschneeberger.shizustore.data.room.WindowAwarePagingSource
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.AppDownloadDao
import me.timschneeberger.shizustore.data.room.dao.CategoryDao
import me.timschneeberger.shizustore.data.room.dao.InstalledDao
import me.timschneeberger.shizustore.data.room.dao.SyncStateDao
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.CategoryEntity
import me.timschneeberger.shizustore.util.SHIZU_STORE_PACKAGE

@Singleton
class AppRepository @Inject constructor(
    private val appDao: AppDao,
    private val appDownloadDao: AppDownloadDao,
    private val categoryDao: CategoryDao,
    private val syncStateDao: SyncStateDao,
    private val installedDao: InstalledDao,
    private val mapper: CatalogUiMapper
) {
    fun pagedApps(
        args: AppListArgs,
        useInstallCountsForPopularity: Boolean = false
    ): PagingSource<Int, AppEntity> = WindowAwarePagingSource(
        appDao.pagedFiltered(
            AppListQueryBuilder.build(
                args,
                useInstallCountsForPopularity,
                excludePackage = SHIZU_STORE_PACKAGE
            )
        )
    )

    // Room re-emits on every sync_state write; the flag must not rebuild the Pager
    // when its value is unchanged, or the cached paging window is lost.
    fun observePopularityFlag(): Flow<Boolean> = syncStateDao.observe()
        .map { it?.useInstallCountsForPopularity == true }
        .distinctUntilChanged()

    fun pagedInstalled(): PagingSource<Int, AppEntity> = appDao.pagedInstalled()

    fun pagedUpdatable(): PagingSource<Int, AppEntity> = appDao.pagedUpdatable()

    fun observeUpdatableCount(): Flow<Int> = appDao.observeUpdatableCount()

    fun observeRecentlyAdded(limit: Int = CAROUSEL_LIMIT): Flow<List<AppEntity>> =
        appDao.observeRecentlyAdded(limit).hideSelf()

    fun observeRecommendedPool(): Flow<List<AppEntity>> = appDao.observeRecommendedPool().hideSelf()

    fun observeByAuthor(
        authorKey: String,
        excludeSlug: String,
        limit: Int = CAROUSEL_LIMIT
    ): Flow<List<AppEntity>> = appDao.observeByAuthor(authorKey, excludeSlug, limit).hideSelf()

    fun observeByCategory(
        categorySlug: String,
        excludeSlug: String,
        limit: Int = CAROUSEL_LIMIT
    ): Flow<List<AppEntity>> = appDao.observeByCategory(categorySlug, excludeSlug, limit).hideSelf()

    fun observeRecentlyUpdated(limit: Int = CAROUSEL_LIMIT): Flow<List<AppEntity>> =
        appDao.observeRecentlyUpdated(limit).hideSelf()

    fun observeMostStarred(limit: Int = CAROUSEL_LIMIT): Flow<List<AppEntity>> =
        appDao.observeMostStarred(limit).hideSelf()

    fun observePopular(limit: Int = CAROUSEL_LIMIT): Flow<List<AppEntity>> =
        appDao.observePopular(limit).hideSelf()

    fun observeRandomPool(): Flow<List<AppEntity>> = appDao.observeAll().hideSelf()

    fun observeDetail(slug: String): Flow<DetailedApp?> = combine(
        appDao.observe(slug),
        appDownloadDao.observeForApp(slug)
    ) { app, downloads ->
        app?.let {
            DetailedApp(it, downloads.map { entity -> AppCandidate.from(entity, it.packageName) })
        }
    }

    suspend fun get(slug: String): AppEntity? = appDao.get(slug)

    /** Resolves a navigation key: slugs are exact, package names cover legacy links. */
    suspend fun findByKey(key: String): AppEntity? = appDao.get(key) ?: resolveByPackage(key)

    /**
     * Package names are not unique. Prefer the listing whose signing key matches the
     * installed app, then fall back to the deterministic catalog order.
     */
    suspend fun resolveByPackage(packageName: String): AppEntity? {
        val apps = appDao.getAllByPackage(packageName)
        if (apps.size <= 1) return apps.firstOrNull()

        val candidatesByApp = apps.associateWith { app ->
            appDownloadDao.forApp(app.slug).map { AppCandidate.from(it, app.packageName) }
        }
        val packages = buildSet {
            apps.forEach { app -> app.packageName?.let(::add) }
            candidatesByApp.values.flatten().forEach { candidate ->
                candidate.packageName?.let(::add)
            }
        }
        val installedByPackage = installedDao.getByPackages(packages.toList())
            .associateBy { it.packageName }
        return apps.firstOrNull { app ->
            matchInstalled(app.packageName, candidatesByApp[app].orEmpty(), installedByPackage) !=
                null
        } ?: apps.first()
    }

    /**
     * The package the entry is actually installed under: the canonical package or,
     * when a flavor is installed, that flavor's package. Installed-app actions
     * (open, uninstall, app info) must target it rather than the catalog key.
     */
    suspend fun installedPackageFor(slug: String): String? {
        val app = appDao.get(slug) ?: return null
        val candidates = appDownloadDao.forApp(slug).map { AppCandidate.from(it, app.packageName) }
        val packages = buildSet {
            app.packageName?.let(::add)
            candidates.forEach { candidate -> candidate.packageName?.let(::add) }
        }
        if (packages.isEmpty()) return null
        val installedByPackage = installedDao.getByPackages(packages.toList())
            .associateBy { it.packageName }
        return matchInstalled(app.packageName, candidates, installedByPackage)?.packageName
    }

    /** Updatable rows with a real package name, the only ones an install can target. */
    suspend fun updatableApps(): List<AppEntity> = appDao.getUpdatable()

    /** Whole installed set for bulk actions such as the Obtainium export. */
    suspend fun installedApps(): List<AppEntity> = appDao.getInstalled()

    suspend fun candidate(id: Long): AppCandidate? = appDownloadDao.getById(id)?.let { entity ->
        val packageName = appDao.get(entity.appSlug)?.packageName
        AppCandidate.from(entity, packageName)
    }

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    /** Whole catalog for the home category sections; self-filtered like the pools. */
    fun observeAllApps(): Flow<List<ResolvedApp>> =
        appDao.observeAll().hideSelf().map { apps -> apps.map(mapper::toResolvedApp) }

    private fun Flow<List<AppEntity>>.hideSelf(): Flow<List<AppEntity>> =
        map { apps -> apps.filterNot { it.packageName == SHIZU_STORE_PACKAGE } }

    companion object {
        const val CAROUSEL_LIMIT = 20
    }
}
