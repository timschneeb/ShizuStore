/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import androidx.paging.PagingSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.FavouriteDao

@Singleton
class FavouriteRepository @Inject constructor(
    private val favouriteDao: FavouriteDao,
    private val appDao: AppDao,
    private val mapper: CatalogUiMapper
) {

    fun observeFavourites(): Flow<List<String>> = favouriteDao.observeAll()

    fun isFavourite(packageName: String): Flow<Boolean> =
        favouriteDao.observeIsFavourite(packageName)

    suspend fun toggle(packageName: String) = favouriteDao.toggle(packageName)

    fun pagedFavourites(): PagingSource<Int, ResolvedApp> =
        MappedPagingSource(appDao.pagedFavourites()) { mapper.toResolvedApp(it) }
}
