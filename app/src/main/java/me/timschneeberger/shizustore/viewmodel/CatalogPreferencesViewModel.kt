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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.sync.CatalogSyncer
import me.timschneeberger.shizustore.data.work.SyncWorker
import me.timschneeberger.shizustore.util.Preferences
import me.timschneeberger.shizustore.util.Preferences.PREFERENCE_SHOW_CLOSED_SOURCE
import me.timschneeberger.shizustore.util.Preferences.PREFERENCE_SHOW_TRACKER_INFO

@HiltViewModel
class CatalogPreferencesViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val syncer: CatalogSyncer
) : ViewModel() {

    private val _showClosedSource = MutableStateFlow(false)

    val showClosedSource: StateFlow<Boolean> = _showClosedSource.asStateFlow()

    private val _showTrackerInfo = MutableStateFlow(true)

    val showTrackerInfo: StateFlow<Boolean> = _showTrackerInfo.asStateFlow()

    init {
        viewModelScope.launch {
            Preferences.booleanFlow(context, PREFERENCE_SHOW_CLOSED_SOURCE, false).collect {
                _showClosedSource.value = it
            }
        }
        viewModelScope.launch {
            Preferences.booleanFlow(context, PREFERENCE_SHOW_TRACKER_INFO, true).collect {
                _showTrackerInfo.value = it
            }
        }
    }

    fun setShowClosedSource(enabled: Boolean) {
        viewModelScope.launch {
            Preferences.putBoolean(context, PREFERENCE_SHOW_CLOSED_SOURCE, enabled)
            syncer.onShowClosedSourceChanged(enabled)
            SyncWorker.enqueue(context)
        }
    }

    fun setShowTrackerInfo(enabled: Boolean) {
        viewModelScope.launch {
            Preferences.putBoolean(context, PREFERENCE_SHOW_TRACKER_INFO, enabled)
        }
    }
}
