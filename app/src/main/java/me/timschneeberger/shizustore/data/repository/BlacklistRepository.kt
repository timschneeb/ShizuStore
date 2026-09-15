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
import me.timschneeberger.shizustore.data.room.dao.BlacklistDao

@Singleton
class BlacklistRepository @Inject constructor(
    private val blacklistDao: BlacklistDao,
    private val appDao: AppDao,
    private val mapper: CatalogUiMapper
) {

    fun observeBlacklist(): Flow<List<String>> = blacklistDao.observeAll()

    fun isBlacklisted(packageName: String): Flow<Boolean> =
        blacklistDao.observeIsBlacklisted(packageName)

    suspend fun toggle(packageName: String) = blacklistDao.toggle(packageName)

    fun observeBlacklistedApps(): Flow<List<ResolvedApp>> =
        appDao.observeBlacklisted().map { apps -> apps.map(mapper::toResolvedApp) }
}
