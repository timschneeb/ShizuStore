/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.api.ApiResult
import me.timschneeberger.shizustore.data.api.BaseUrlProvider
import me.timschneeberger.shizustore.data.api.ShizuApi
import me.timschneeberger.shizustore.data.helper.SyncHelper
import me.timschneeberger.shizustore.util.Preferences
import me.timschneeberger.shizustore.util.Preferences.PREFERENCE_API_BASE_URL
import me.timschneeberger.shizustore.util.Preferences.PREFERENCE_API_SERVER_CUSTOM
import me.timschneeberger.shizustore.util.ServerConfig

enum class ServerStatus { UNKNOWN, CHECKING, REACHABLE, UNREACHABLE }

@HiltViewModel
class ServerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    baseUrlProvider: BaseUrlProvider,
    private val api: ShizuApi,
    private val syncHelper: SyncHelper
) : ViewModel() {

    val useCustom: StateFlow<Boolean> = Preferences
        .booleanFlow(context, PREFERENCE_API_SERVER_CUSTOM)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    val customUrl: StateFlow<String> = Preferences
        .stringFlow(context, PREFERENCE_API_BASE_URL)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), "")

    val baseUrl: StateFlow<String> = baseUrlProvider.observe().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        ServerConfig.baseUrl
    )

    private val _status = MutableStateFlow(ServerStatus.UNKNOWN)
    val status: StateFlow<ServerStatus> = _status.asStateFlow()

    init {
        validate()
    }

    /** Switches between the production server and the stored custom one. */
    fun setUseCustom(enabled: Boolean) {
        viewModelScope.launch {
            Preferences.putBoolean(context, PREFERENCE_API_SERVER_CUSTOM, enabled)
            validate()
        }
    }

    /** Returns false when [raw] is not a usable http(s) base URL. */
    fun saveCustomUrl(raw: String): Boolean {
        val normalized = normalize(raw) ?: return false
        viewModelScope.launch {
            Preferences.putString(context, PREFERENCE_API_BASE_URL, normalized)
            Preferences.putBoolean(context, PREFERENCE_API_SERVER_CUSTOM, true)
            validate()
        }
        return true
    }

    /** Wipes the cached catalog and pulls everything again from the active server. */
    fun clearLocalDatabase() {
        syncHelper.clearLocalDatabase()
    }

    fun validate() {
        viewModelScope.launch {
            _status.value = ServerStatus.CHECKING
            _status.value = when (api.health()) {
                is ApiResult.Success -> ServerStatus.REACHABLE
                is ApiResult.Failure -> ServerStatus.UNREACHABLE
            }
        }
    }

    private fun normalize(raw: String): String? {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isBlank()) return null
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
        return trimmed
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
