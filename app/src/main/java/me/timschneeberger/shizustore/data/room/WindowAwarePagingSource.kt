/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Rebases the refresh key onto the loaded window.
 *
 * Room derives the refresh key from the presented anchor, but with placeholders disabled Paging
 * makes that anchor relative to the loaded window. Once the window has been rebased (its first
 * page is no longer at offset 0), a hint-less or near-top refresh asks for a key close to zero
 * and the list resets to the first page. Room stores each page's absolute offset as its prevKey,
 * so shifting the delegate key by the window start keeps refreshes on the row the user was on.
 */
internal class WindowAwarePagingSource<Value : Any>(
    private val delegate: PagingSource<Int, Value>
) : PagingSource<Int, Value>() {

    init {
        // The delegate invalidates itself on database writes; forward that to our callbacks.
        delegate.registerInvalidatedCallback { invalidate() }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Value> =
        delegate.load(params)

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        val firstPage = state.pages.firstOrNull { it.data.isNotEmpty() }
            ?: return delegate.getRefreshKey(state)
        val windowStart = firstPage.prevKey ?: 0
        // A null key means Paging had no anchor; keeping the current window beats resetting.
        val delegateKey = delegate.getRefreshKey(state) ?: return windowStart
        return windowStart + delegateKey
    }

    override val jumpingSupported: Boolean
        get() = delegate.jumpingSupported

    override val keyReuseSupported: Boolean
        get() = delegate.keyReuseSupported
}
