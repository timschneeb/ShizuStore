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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.repository.BlacklistRepository

@HiltViewModel
class BlacklistViewModel @Inject constructor(
    private val blacklistRepository: BlacklistRepository
) : ViewModel() {

    val apps: StateFlow<List<ResolvedApp>?> = blacklistRepository.observeBlacklistedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val hasBlacklistedPackages: StateFlow<Boolean?> = blacklistRepository.observeBlacklist()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun unblacklist(packageName: String) {
        viewModelScope.launch { blacklistRepository.toggle(packageName) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
