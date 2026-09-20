/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.helper.SyncHelper
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.repository.AppRepository
import me.timschneeberger.shizustore.data.repository.CatalogUiMapper
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure
import me.timschneeberger.shizustore.util.Preferences

@HiltViewModel
class AppsViewModel @Inject constructor(
    appRepository: AppRepository,
    mapper: CatalogUiMapper,
    private val syncHelper: SyncHelper,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    val syncing: StateFlow<Boolean> = syncHelper.syncing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    /** True only for a user-triggered refresh; background syncs stay invisible. */
    val refreshing: StateFlow<Boolean> = syncHelper.refreshing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    /** Set when the last catalog pass failed; drives the home error + retry. */
    val syncFailure: StateFlow<CatalogSyncFailure?> = syncHelper.failure
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun retrySync() {
        // Reseed so a user refresh visibly rotates the curated rows; background syncs
        // keep the current order.
        recommendedSeed.value = Random.nextLong()
        randomSeed.value = Random.nextLong()
        syncHelper.refresh()
    }

    val shizukuCardDismissed: StateFlow<Boolean> =
        Preferences.booleanFlow(context, Preferences.PREFERENCE_SHIZUKU_CARD_DISMISSED)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    fun dismissShizukuCard() {
        viewModelScope.launch {
            Preferences.putBoolean(context, Preferences.PREFERENCE_SHIZUKU_CARD_DISMISSED, true)
        }
    }

    // Seeded per ViewModel (app launch) and reseeded on a user refresh: the rows vary
    // between launches but do not reshuffle on every catalog emission while a sync
    // writes to Room.
    private val recommendedSeed = MutableStateFlow(Random.nextLong())
    private val randomSeed = MutableStateFlow(daySeed())

    private val recommendedPicks: Flow<List<ResolvedApp>> =
        combine(appRepository.observeRecommendedPool(), recommendedSeed) { pool, seed ->
            pool.sortedBy { it.slug }
                .shuffled(Random(seed))
                .take(AppRepository.CAROUSEL_LIMIT)
                .map(mapper::toResolvedApp)
        }

    private val randomPicks: Flow<List<ResolvedApp>> =
        combine(appRepository.observeRandomPool(), randomSeed) { pool, seed ->
            pool.sortedBy { it.slug }
                .shuffled(Random(seed))
                .take(AppRepository.CAROUSEL_LIMIT)
                .map(mapper::toResolvedApp)
        }

    val groups: StateFlow<List<AppGroup>?> = combine(
        recommendedPicks,
        appRepository.observeRecentlyAdded().map { it.map(mapper::toResolvedApp) },
        appRepository.observeRecentlyUpdated().map { it.map(mapper::toResolvedApp) },
        appRepository.observeMostStarred().map { it.map(mapper::toResolvedApp) },
        randomPicks
    ) { recommended, recentlyAdded, recentlyUpdated, mostStarred, picks ->
        listOf(
            AppGroup(AppGroupKind.RECOMMENDED, recommended),
            AppGroup(AppGroupKind.RECENTLY_ADDED, recentlyAdded),
            AppGroup(AppGroupKind.RECENTLY_UPDATED, recentlyUpdated),
            AppGroup(AppGroupKind.MOST_STARRED, mostStarred),
            AppGroup(AppGroupKind.RANDOM_PICKS, picks)
        ).filter { it.apps.isNotEmpty() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        /** `Calendar`, not `LocalDate`: that is API 26, `minSdk` is 24 and desugaring is off. */
        fun daySeed(): Long = Calendar.getInstance().let { now ->
            now.get(Calendar.YEAR) * 1_000L + now.get(Calendar.DAY_OF_YEAR)
        }
    }
}
