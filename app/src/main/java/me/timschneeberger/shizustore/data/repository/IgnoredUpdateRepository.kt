/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.IgnoredUpdateDao
import me.timschneeberger.shizustore.data.room.entity.IgnoredUpdateEntity

@Singleton
class IgnoredUpdateRepository @Inject constructor(
    private val ignoredUpdateDao: IgnoredUpdateDao,
    private val appDao: AppDao,
    private val mapper: CatalogUiMapper,
    private val updateStateRepository: UpdateStateRepository
) {

    fun observeIgnores(): Flow<List<IgnoredUpdateEntity>> = ignoredUpdateDao.observeAll()

    fun observeIgnore(packageName: String): Flow<IgnoredUpdateEntity?> =
        ignoredUpdateDao.observe(packageName)

    fun observeIgnoredApps(): Flow<List<ResolvedApp>> =
        appDao.observeIgnoredUpdates().map { apps -> apps.map(mapper::toResolvedApp) }

    suspend fun ignoreAll(packageName: String) {
        ignoredUpdateDao.upsert(IgnoredUpdateEntity(canonicalPackage(packageName), null))
        updateStateRepository.recompute(packageName)
    }

    /** Stores the version the update flow currently offers, so suppression matches exactly. */
    suspend fun ignoreVersion(packageName: String) {
        val offered = updateStateRepository.offeredVersionCode(packageName) ?: return
        ignoredUpdateDao.upsert(IgnoredUpdateEntity(canonicalPackage(packageName), offered))
        updateStateRepository.recompute(packageName)
    }

    suspend fun stopIgnoring(packageName: String) {
        ignoredUpdateDao.delete(canonicalPackage(packageName))
        ignoredUpdateDao.delete(packageName)
        updateStateRepository.recompute(packageName)
    }

    /** Rows are keyed by the app's canonical package, never by an installed flavor package. */
    private suspend fun canonicalPackage(packageName: String): String =
        (appDao.getByPackage(packageName) ?: appDao.getByDownloadPackage(packageName))
            ?.packageName
            ?: packageName
}
