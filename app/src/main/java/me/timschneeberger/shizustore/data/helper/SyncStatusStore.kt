/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure

/**
 * Last catalog sync failure for the running process so the UI can show an error
 * instead of an endless spinner.
 */
@Singleton
class SyncStatusStore @Inject constructor() {
    private val _failure = MutableStateFlow<CatalogSyncFailure?>(null)
    private val _iconGeneration = MutableStateFlow(0)
    private val _manualRefreshing = MutableStateFlow(false)

    val failure: StateFlow<CatalogSyncFailure?> = _failure.asStateFlow()

    /**
     * True only for a refresh the user asked for; background syncs also run but
     * must not blank the list or show the pull-to-refresh spinner.
     */
    val manualRefreshing: StateFlow<Boolean> = _manualRefreshing.asStateFlow()

    /**
     * Bumped on every refresh so app icons re-run Coil; a failed icon is not cached
     * on disk but can linger in Coil's memory cache, so the key forces a network retry.
     */
    val iconGeneration: StateFlow<Int> = _iconGeneration.asStateFlow()

    fun set(failure: CatalogSyncFailure?) {
        _failure.value = failure
    }

    fun refreshIcons() {
        _iconGeneration.value += 1
    }

    fun setManualRefreshing(refreshing: Boolean) {
        _manualRefreshing.value = refreshing
    }
}
