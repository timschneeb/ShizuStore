/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.timschneeberger.shizustore.data.helper.DownloadHelper
import me.timschneeberger.shizustore.data.helper.InstallDispatcher
import me.timschneeberger.shizustore.data.model.InstallDispatch
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.util.PathUtil

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadHelper: DownloadHelper,
    private val installDispatcher: InstallDispatcher
) : ViewModel() {
    val downloads: Flow<PagingData<Download>> =
        Pager(PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false)) {
            downloadHelper.pagedDownloads()
        }.flow.cachedIn(viewModelScope)

    private val _refusals = Channel<InstallDispatch.Refused>(Channel.BUFFERED)
    val refusals: Flow<InstallDispatch.Refused> = _refusals.receiveAsFlow()

    private val _exports = Channel<Boolean>(Channel.BUFFERED)
    val exports: Flow<Boolean> = _exports.receiveAsFlow()

    fun canInstall(download: Download): Boolean = installDispatcher.canInstallFromDisk(download)

    fun cancel(packageName: String) {
        launchDetached { downloadHelper.cancel(packageName) }
    }

    fun cancelAll() {
        launchDetached { downloadHelper.cancelAll() }
    }

    fun clear(packageName: String) {
        launchDetached { downloadHelper.remove(packageName) }
    }

    fun clearFinished() {
        launchDetached { downloadHelper.clearFinished() }
    }

    fun clearAll() {
        launchDetached { downloadHelper.clearAll() }
    }

    fun install(packageName: String) {
        launchDetached {
            when (val dispatch = installDispatcher.dispatch(packageName)) {
                InstallDispatch.Started -> Unit
                is InstallDispatch.Refused -> _refusals.send(dispatch)
            }
        }
    }

    fun export(packageName: String, target: Uri) {
        launchDetached {
            val download = downloadHelper.getDownload(packageName)
            if (download == null) {
                Log.w(TAG, "Not exporting $packageName; it has no download row")
                _exports.send(false)
                return@launchDetached
            }

            val exported = withContext(Dispatchers.IO) {
                runCatching {
                    val apk = PathUtil.getApkFile(
                        context,
                        download.packageName,
                        download.versionCode
                    )
                    val stream = context.contentResolver.openOutputStream(target)
                        ?: error("the picker returned a URI that cannot be written")
                    stream.use { out -> apk.inputStream().use { it.copyTo(out) } }
                }.onFailure {
                    Log.e(TAG, "Could not export $packageName", it)
                }.isSuccess
            }
            _exports.send(exported)
        }
    }

    /** Runs the mutation to completion even if the screen is left. */
    private fun launchDetached(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch { withContext(NonCancellable, block) }
    }

    private companion object {
        const val TAG = "DownloadsViewModel"
    }
}
