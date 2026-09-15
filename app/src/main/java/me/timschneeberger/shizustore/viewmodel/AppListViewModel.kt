/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.helper.SyncHelper
import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.data.model.AppPrice
import me.timschneeberger.shizustore.data.model.AppSort
import me.timschneeberger.shizustore.data.model.CategoryTag
import me.timschneeberger.shizustore.data.model.CategoryTagTree
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.repository.AppRepository
import me.timschneeberger.shizustore.data.repository.CatalogUiMapper
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure
import me.timschneeberger.shizustore.util.SearchHistoryStore

/** One list for search, category, recently added/updated and the other sort presets. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AppListViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val mapper: CatalogUiMapper,
    private val searchHistory: SearchHistoryStore,
    private val syncHelper: SyncHelper
) : ViewModel() {

    private val _args = MutableStateFlow(AppListArgs())

    /** Search home is an explicit place, not "all filters are clear", so clearing
     * the last filter keeps the user on the list instead of jumping home. */
    private val _atSearchHome = MutableStateFlow(true)

    private var initialized = false

    val args: StateFlow<AppListArgs> = _args.asStateFlow()

    val atSearchHome: StateFlow<Boolean> = _atSearchHome.asStateFlow()

    val syncing: StateFlow<Boolean> = syncHelper.syncing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    /** True only for a user-triggered refresh; background syncs stay invisible. */
    val refreshing: StateFlow<Boolean> = syncHelper.refreshing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    /** Set when the last catalog pass failed; drives the list's error + retry. */
    val syncFailure: StateFlow<CatalogSyncFailure?> = syncHelper.failure
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val categories: StateFlow<List<CategoryTag>?> = appRepository.observeCategories()
        .map(CategoryTagTree::build)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val history: StateFlow<List<String>> = searchHistory.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val apps: Flow<PagingData<ResolvedApp>> = combine(
        _args,
        appRepository.observePopularityFlag()
    ) { args, flag -> args to flag }
        .flatMapLatest { (args, flag) ->
            Pager(PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) {
                appRepository.pagedApps(args, flag)
            }.flow.map { paging -> paging.map(mapper::toResolvedApp) }
        }
        .cachedIn(viewModelScope)

    /**
     * Server feature flag for the popularity sort: when true the DOWNLOADS
     * sort orders by client-reported install counts and the list subtitle
     * shows them instead of the upstream download totals.
     */
    val useInstallCountsForPopularity: StateFlow<Boolean> =
        appRepository.observePopularityFlag()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    /**
     * Applies the navigation preset once. Re-entering composition (back from a
     * detail page, configuration change) must not reset the query/filters and
     * flash the unfiltered list.
     */
    fun initialize(args: AppListArgs) {
        if (initialized) return
        initialized = true
        _args.value = args
    }

    fun setQuery(query: String) {
        _atSearchHome.value = false
        _args.update { it.copy(query = query) }
        if (query.isNotBlank()) {
            viewModelScope.launch { searchHistory.record(query) }
        }
    }

    fun setCategory(categorySlug: String?) {
        _atSearchHome.value = false
        _args.update { it.copy(categorySlug = categorySlug) }
    }

    fun setPrice(price: AppPrice?) {
        _atSearchHome.value = false
        _args.update { it.copy(price = price) }
    }

    fun setRecommended(recommended: Boolean) {
        _atSearchHome.value = false
        _args.update { it.copy(recommended = recommended) }
    }

    fun setSort(sort: AppSort) {
        _atSearchHome.value = false
        _args.update { it.copy(sort = sort) }
    }

    fun clearFilters() {
        _atSearchHome.value = false
        _args.update { it.copy(categorySlug = null, recommended = false, sort = AppSort.NAME) }
    }

    /** Back from a search/list leaves only the untouched search home. */
    fun clearAll() {
        _args.value = AppListArgs()
        _atSearchHome.value = true
    }

    fun clearHistory() {
        viewModelScope.launch { searchHistory.clear() }
    }

    fun retrySync() = syncHelper.refresh()

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
