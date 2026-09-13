/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Paging 3.5.0 only offers `PagingData.map`, not a `PagingSource` transform, so this adapts a
 * source of Room entities into the presentation model without re-querying.
 */
internal class MappedPagingSource<Key : Any, Value : Any, R : Any>(
    private val delegate: PagingSource<Key, Value>,
    private val transform: (Value) -> R
) : PagingSource<Key, R>() {

    override suspend fun load(params: LoadParams<Key>): LoadResult<Key, R> =
        when (val result = delegate.load(params)) {
            is LoadResult.Page -> LoadResult.Page(
                data = result.data.map(transform),
                prevKey = result.prevKey,
                nextKey = result.nextKey
            )

            is LoadResult.Error -> LoadResult.Error(result.throwable)
            is LoadResult.Invalid -> LoadResult.Invalid()
        }

    override fun getRefreshKey(state: PagingState<Key, R>): Key? = null
}
