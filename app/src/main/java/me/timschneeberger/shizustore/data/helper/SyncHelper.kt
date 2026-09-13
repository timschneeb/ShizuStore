/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure
import me.timschneeberger.shizustore.data.work.SyncWorker

/** Global catalog sync control; there is a single remote, so no per-repo state. */
@Singleton
open class SyncHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val syncStatus: SyncStatusStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Clear the user-refresh flag once every tagged sync has finished, so the
        // pull-to-refresh spinner ends even if the work outlived the caller.
        scope.launch {
            WorkManager.getInstance(context).getWorkInfosByTagFlow(SyncWorker.TAG)
                .collect { infos ->
                    if (infos.none { !it.state.isFinished }) {
                        syncStatus.setManualRefreshing(false)
                    }
                }
        }
    }

    /**
     * True while a catalog sync is actually running. Work that is only queued
     * for a retry backoff is not counted, otherwise a scheduled retry would keep
     * the pull-to-refresh spinner on forever after the manual attempt failed.
     */
    open val syncing: Flow<Boolean> = WorkManager.getInstance(context)
        .getWorkInfosByTagFlow(SyncWorker.TAG)
        .map { infos -> infos.any { it.state == WorkInfo.State.RUNNING } }

    /** True only for a refresh the user triggered, never for a background sync. */
    open val refreshing: Flow<Boolean> = syncStatus.manualRefreshing

    /** Last failure, so the UI can show an error instead of an endless spinner. */
    open val failure: Flow<CatalogSyncFailure?> = syncStatus.failure

    /** Bumped on every refresh so app icons that failed to load are retried. */
    open val iconGeneration: Flow<Int> = syncStatus.iconGeneration

    /**
     * Enqueues a background catalog sync. Runs silently: the auto-sync on launch
     * and the periodic syncs must not blank the list or show a pull spinner.
     */
    open fun sync() {
        if (!isOnline()) {
            syncStatus.set(CatalogSyncFailure.NETWORK)
            return
        }
        syncStatus.refreshIcons()
        SyncWorker.enqueue(context, expedited = true)
    }

    /**
     * A user-initiated refresh (pull to refresh, retry, empty-state action). Same
     * sync as [sync], but the UI shows the pull-to-refresh feedback until it ends.
     */
    open fun refresh() {
        if (!isOnline()) {
            syncStatus.set(CatalogSyncFailure.NETWORK)
            return
        }
        syncStatus.refreshIcons()
        syncStatus.setManualRefreshing(true)
        SyncWorker.enqueue(context, expedited = true)
    }

    open fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.UNIQUE_NAME)
    }

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
