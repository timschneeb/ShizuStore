/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.helper.DownloadHelper
import me.timschneeberger.shizustore.data.helper.SyncHelper
import me.timschneeberger.shizustore.data.helper.UpdateBatch
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.repository.AppRepository
import me.timschneeberger.shizustore.data.repository.BlacklistRepository
import me.timschneeberger.shizustore.data.repository.CatalogUiMapper
import me.timschneeberger.shizustore.data.repository.DetailedAppRepository
import me.timschneeberger.shizustore.data.repository.DetailedAppResult
import me.timschneeberger.shizustore.data.repository.IgnoredUpdateRepository
import me.timschneeberger.shizustore.data.repository.UpdateStateRepository
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val detailedAppRepository: DetailedAppRepository,
    private val updateStateRepository: UpdateStateRepository,
    private val mapper: CatalogUiMapper,
    private val updateBatch: UpdateBatch,
    private val ignoredUpdateRepository: IgnoredUpdateRepository,
    private val blacklistRepository: BlacklistRepository,
    private val downloadHelper: DownloadHelper,
    private val syncHelper: SyncHelper
) : ViewModel() {

    val updates: Flow<PagingData<ResolvedApp>> =
        Pager(PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) {
            appRepository.pagedUpdatable()
        }.flow.map { paging ->
            paging.map { entity ->
                // Carry the package the app is actually installed under so the
                // row's download/progress and actions target the installed flavor.
                mapper.toResolvedApp(entity).copy(
                    installedPackage = appRepository.installedPackageFor(entity.slug)
                )
            }
        }.cachedIn(viewModelScope)

    val downloadsByPackage: StateFlow<Map<String, Download>> = downloadHelper.downloads
        .map { downloads -> downloads.associateBy { it.packageName } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyMap())

    val anyDownloadActive: StateFlow<Boolean> = downloadHelper.downloads
        .map { downloads ->
            downloads.any { it.isActive || it.isInstalling }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val updateCount: StateFlow<Int> = appRepository.observeUpdatableCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    /** Drives pull to refresh; updates are computed from the catalog, so a sync refreshes them. */
    val syncing: StateFlow<Boolean> = syncHelper.syncing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    /** True only for a user-triggered refresh; background syncs stay invisible. */
    val refreshing: StateFlow<Boolean> = syncHelper.refreshing
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val syncFailure: StateFlow<CatalogSyncFailure?> = syncHelper.failure
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val blacklisted: StateFlow<Set<String>> = blacklistRepository.observeBlacklist()
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    private val _sheetApp = MutableStateFlow<ResolvedApp?>(null)
    val sheetApp: StateFlow<ResolvedApp?> = _sheetApp.asStateFlow()

    fun update(app: ResolvedApp) {
        viewModelScope.launch {
            val entity = appRepository.get(app.slug) ?: return@launch
            enqueueUpdatable(entity, unattended = false)
        }
    }

    fun updateAll() {
        viewModelScope.launch {
            appRepository.updatableApps().forEach { enqueueUpdatable(it, unattended = false) }
        }
    }

    fun cancel(packageName: String) {
        viewModelScope.launch { downloadHelper.cancel(packageName) }
    }

    fun cancelAll() {
        viewModelScope.launch { updateBatch.cancelAll() }
    }

    fun retrySync() = syncHelper.refresh()

    fun ignoreThisVersion(app: ResolvedApp) {
        viewModelScope.launch {
            ignoredUpdateRepository.ignoreVersion(app.packageName, app.versionCode)
        }
    }

    fun ignoreAllUpdates(app: ResolvedApp) {
        viewModelScope.launch { ignoredUpdateRepository.ignoreAll(app.packageName) }
    }

    fun toggleBlacklist(app: ResolvedApp) {
        viewModelScope.launch { blacklistRepository.toggle(app.packageName) }
    }

    fun openSheet(app: ResolvedApp) {
        _sheetApp.value = app
    }

    fun dismissSheet() {
        _sheetApp.value = null
    }

    /** Resolves the signature-matching candidate for an updatable row, fetching detail if needed. */
    private suspend fun enqueueUpdatable(app: AppEntity, unattended: Boolean): Boolean {
        val packageName = app.packageName ?: return false

        var current = app
        if (current.updateCandidateId == null) {
            when (detailedAppRepository.fetchAndPersist(current.slug)) {
                is DetailedAppResult.Success -> Unit
                else -> return false
            }
            updateStateRepository.recompute(packageName)
            current = appRepository.get(current.slug) ?: return false
        }

        val candidate: AppCandidate =
            appRepository.candidate(current.updateCandidateId ?: return false)
                ?: return false

        return runCatching { updateBatch.enqueue(current, candidate, unattended) }
            .onFailure { Log.w(TAG, "Could not enqueue update for $packageName", it) }
            .getOrDefault(false)
    }

    private companion object {
        const val TAG = "UpdatesViewModel"
    }
}
