/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.repository.IgnoredUpdateRepository

data class IgnoredUpdate(val app: ResolvedApp, val ignoredVersionCode: Long?)

@HiltViewModel
class IgnoredUpdatesViewModel @Inject constructor(
    private val ignoredUpdateRepository: IgnoredUpdateRepository
) : ViewModel() {

    val ignored: StateFlow<List<IgnoredUpdate>?> = combine(
        ignoredUpdateRepository.observeIgnoredApps(),
        ignoredUpdateRepository.observeIgnores()
    ) { apps, ignores ->
        val byPackage = ignores.associateBy { it.packageName }
        apps.mapNotNull { app ->
            byPackage[app.packageName]?.let { IgnoredUpdate(app, it.versionCode) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val hasIgnoredPackages: StateFlow<Boolean?> = ignoredUpdateRepository.observeIgnores()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun stopIgnoring(packageName: String) {
        viewModelScope.launch { ignoredUpdateRepository.stopIgnoring(packageName) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
