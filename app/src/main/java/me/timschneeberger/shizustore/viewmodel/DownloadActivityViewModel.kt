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
import me.timschneeberger.shizustore.data.helper.DownloadHelper

/**
 * Watches the downloads table rather than hooking each button, since a download can start from
 * the details screen, the manual download screen, the updates list and the update sheet.
 */
@HiltViewModel
class DownloadActivityViewModel @Inject constructor(
    downloadHelper: DownloadHelper
) : ViewModel() {

    val isDownloading: StateFlow<Boolean> = downloadHelper.downloads
        .map { downloads -> downloads.any { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)
}
