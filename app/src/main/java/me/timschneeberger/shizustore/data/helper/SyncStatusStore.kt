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
 * Last catalog sync failure for the running process, so the UI can show a real
 * error with a retry instead of spinning on "syncing" forever. Cleared on the
 * next successful pass. In-memory only: a restart starts fresh.
 */
@Singleton
class SyncStatusStore @Inject constructor() {
    private val _failure = MutableStateFlow<CatalogSyncFailure?>(null)
    private val _iconGeneration = MutableStateFlow(0)
    private val _manualRefreshing = MutableStateFlow(false)

    val failure: StateFlow<CatalogSyncFailure?> = _failure.asStateFlow()

    /**
     * True only for a refresh the user asked for. The automatic launch and
     * periodic syncs also run, but they must not blank the list or show the
     * pull-to-refresh spinner, otherwise the content flickers on every start.
     */
    val manualRefreshing: StateFlow<Boolean> = _manualRefreshing.asStateFlow()

    /**
     * Bumped on every refresh so app icons re-run Coil. Failed icons are not
     * cached on disk, but Coil may still remember an error in memory; changing
     * the memory key makes them hit the network again.
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
