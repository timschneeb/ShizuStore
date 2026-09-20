/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingSource.LoadParams
import androidx.paging.PagingSource.LoadResult
import androidx.paging.PagingState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WindowAwarePagingSourceTest {

    private val config = PagingConfig(pageSize = 30)

    @Test
    fun rebasesDelegateKeyOntoWindowStart() {
        val state = state(anchorPosition = 40, pages = listOf(page(prevKey = 105)))

        assertEquals(105, wrapped().getRefreshKey(state))
    }

    @Test
    fun addsWindowStartToNonZeroDelegateKey() {
        val state = state(anchorPosition = 145, pages = listOf(page(prevKey = 105)))

        assertEquals(205, wrapped().getRefreshKey(state))
    }

    @Test
    fun keepsDelegateKeyWhenWindowStartsAtZero() {
        val state = state(anchorPosition = 145, pages = listOf(page(prevKey = null)))

        assertEquals(100, wrapped().getRefreshKey(state))
    }

    @Test
    fun keepsWindowStartWhenPagingHasNoAnchor() {
        val state = state(anchorPosition = null, pages = listOf(page(prevKey = 105)))

        assertEquals(105, wrapped().getRefreshKey(state))
    }

    @Test
    fun fallsBackToDelegateWithoutLoadedPages() {
        val state = state(anchorPosition = null, pages = emptyList())

        assertNull(wrapped().getRefreshKey(state))
    }

    @Test
    fun loadIsForwardedToDelegate() = runTest {
        val delegate = FakeSource()
        val params = LoadParams.Refresh(key = 7, loadSize = 30, placeholdersEnabled = false)

        val result = WindowAwarePagingSource(delegate).load(params)

        assertEquals(params, delegate.lastParams)
        assertEquals(listOf("row"), (result as LoadResult.Page).data)
    }

    private fun wrapped(): WindowAwarePagingSource<String> = WindowAwarePagingSource(FakeSource())

    private fun state(anchorPosition: Int?, pages: List<LoadResult.Page<Int, String>>) =
        PagingState(pages, anchorPosition, config, 0)

    private fun page(prevKey: Int?): LoadResult.Page<Int, String> =
        LoadResult.Page(data = listOf("row"), prevKey = prevKey, nextKey = 100)

    private class FakeSource(
        private val initialLoadSize: Int = 90
    ) : PagingSource<Int, String>() {
        var lastParams: LoadParams<Int>? = null

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
            lastParams = params
            return LoadResult.Page(data = listOf("row"), prevKey = null, nextKey = null)
        }

        // Mimics Room's LimitOffsetPagingSource: key = max(0, anchor - initialLoadSize / 2).
        override fun getRefreshKey(state: PagingState<Int, String>): Int? =
            state.anchorPosition?.let { maxOf(0, it - initialLoadSize / 2) }
    }
}
