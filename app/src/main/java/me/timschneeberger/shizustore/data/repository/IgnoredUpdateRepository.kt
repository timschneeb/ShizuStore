/*
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
    private val mapper: CatalogUiMapper
) {

    fun observeIgnores(): Flow<List<IgnoredUpdateEntity>> = ignoredUpdateDao.observeAll()

    fun observeIgnore(packageName: String): Flow<IgnoredUpdateEntity?> =
        ignoredUpdateDao.observe(packageName)

    fun observeIgnoredApps(): Flow<List<ResolvedApp>> =
        appDao.observeIgnoredUpdates().map { apps -> apps.map(mapper::toResolvedApp) }

    suspend fun ignoreAll(packageName: String) =
        ignoredUpdateDao.upsert(IgnoredUpdateEntity(packageName, null))

    suspend fun ignoreVersion(packageName: String, versionCode: Long) =
        ignoredUpdateDao.upsert(IgnoredUpdateEntity(packageName, versionCode))

    suspend fun stopIgnoring(packageName: String) = ignoredUpdateDao.delete(packageName)
}
