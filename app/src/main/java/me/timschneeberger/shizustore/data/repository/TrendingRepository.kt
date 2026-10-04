/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.timschneeberger.shizustore.data.api.ApiResult
import me.timschneeberger.shizustore.data.api.ShizuApi
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.room.dao.AppDao

@Singleton
class TrendingRepository @Inject constructor(
    private val api: ShizuApi,
    private val appDao: AppDao,
    private val mapper: CatalogUiMapper
) {
    private val _trending = MutableStateFlow<List<ResolvedApp>>(emptyList())
    val trending: StateFlow<List<ResolvedApp>> = _trending.asStateFlow()

    /**
     * Re-fetch the ranked window. The ranking is server-computed from
     * app_install_days, so it is refreshed wholesale instead of synced
     * incrementally like the catalog; failures keep the last good ranking.
     */
    suspend fun refresh() {
        val result = api.trending()
        if (result !is ApiResult.Success) return
        val slugs = result.value.items.map { it.slug }
        if (slugs.isEmpty()) {
            _trending.value = emptyList()
            return
        }
        val bySlug = appDao.getBySlugs(slugs).associateBy { it.slug }
        _trending.value = slugs.mapNotNull(bySlug::get).map(mapper::toResolvedApp)
    }
}
