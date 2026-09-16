/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.timschneeberger.shizustore.data.model.ProxyInfo
import me.timschneeberger.shizustore.util.CommonUtil
import me.timschneeberger.shizustore.util.Preferences
import me.timschneeberger.shizustore.util.Preferences.PREFERENCE_PROXY_INFO

@HiltViewModel
class ProxyViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _proxy = MutableStateFlow<ProxyInfo?>(null)

    val proxy: StateFlow<ProxyInfo?> = _proxy.asStateFlow()

    init {
        viewModelScope.launch {
            Preferences.stringFlow(context, PREFERENCE_PROXY_INFO).collect { raw ->
                _proxy.value = decode(raw)
            }
        }
    }

    fun save(url: String): Boolean {
        val info = CommonUtil.parseProxyUrl(url.trim()) ?: return false
        viewModelScope.launch {
            Preferences.putString(context, PREFERENCE_PROXY_INFO, Json.encodeToString(info))
        }
        return true
    }

    fun clear() {
        viewModelScope.launch { Preferences.removeString(context, PREFERENCE_PROXY_INFO) }
    }

    private fun decode(raw: String): ProxyInfo? {
        if (raw.isBlank()) return null
        return runCatching { Json.decodeFromString<ProxyInfo>(raw) }.getOrNull()
    }
}
