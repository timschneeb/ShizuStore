/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.api.ApiResult
import me.timschneeberger.shizustore.data.api.ShizuApi
import me.timschneeberger.shizustore.data.room.dao.AppDao

/**
 * Loads the server's fresh-install window ranking into the `trendScore` column
 * that backs the Trending sort. Wholesale rewrite, like the feed's own refresh:
 * best-effort, so a failed fetch keeps the last ranking instead of blanking it.
 */
@Singleton
class TrendingRepository @Inject constructor(
    private val api: ShizuApi,
    private val appDao: AppDao
) {

    suspend fun refresh() {
        val result = api.trending(days = WINDOW_DAYS, limit = MAX_ITEMS, sort = "installs")
        if (result is ApiResult.Success) {
            appDao.replaceTrendScores(result.value.items.associate { it.slug to it.installs })
        }
    }

    private companion object {
        /** Matches the "last 14 days" label on the sort option. */
        const val WINDOW_DAYS = 14

        /** Server clamp is 100; deeper ranks fall back to the name tiebreak. */
        const val MAX_ITEMS = 100
    }
}
