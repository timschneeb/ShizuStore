/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.work.DownloadWorker
import me.timschneeberger.shizustore.data.work.InstallWorker
import me.timschneeberger.shizustore.data.work.WorkTags
import me.timschneeberger.shizustore.util.Preferences

@Singleton
class UpdateBatch @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadHelper: DownloadHelper
) {
    suspend fun enqueue(
        app: AppEntity,
        candidate: AppCandidate,
        unattended: Boolean = false
    ): Boolean {
        val packageName = candidate.packageName ?: app.packageName ?: return false

        if (packageName in chainedPackages()) {
            Log.i(TAG, "$packageName is already in the live batch; not adding it again")
            return false
        }

        downloadHelper.stageRow(app, candidate)

        val networkType = downloadNetworkType(unattended)
        WorkManager.getInstance(context)
            .beginUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                DownloadWorker.request(packageName, networkType)
            )
            .then(InstallWorker.request(packageName))
            .enqueue()
        return true
    }

    suspend fun cancelAll() {
        val inFlight = downloadHelper.downloads.first()
            .filter { it.isActive || it.isInstalling }

        Log.i(TAG, "Cancelling the batch and its ${inFlight.size} in-flight download(s)")
        inFlight.forEach { downloadHelper.cancel(it.packageName) }

        withContext(Dispatchers.IO) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }
    }

    private suspend fun downloadNetworkType(unattended: Boolean): NetworkType {
        val wifiOnly = Preferences.readBoolean(context, Preferences.PREFERENCE_SYNC_ON_WIFI_ONLY)
        return if (unattended && wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
    }

    private suspend fun chainedPackages(): Set<String> = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(UNIQUE_NAME)
            .get()
            .filterNot { it.state.isFinished }
            .flatMapTo(mutableSetOf()) { info -> info.tags.mapNotNull(WorkTags::packageOf) }
    }

    companion object {
        const val UNIQUE_NAME = "update-batch"

        private const val TAG = "UpdateBatch"
    }
}
