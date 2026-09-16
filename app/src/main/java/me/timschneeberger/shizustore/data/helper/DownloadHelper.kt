/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's DownloadHelper (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.helper

import android.content.Context
import android.util.Log
import androidx.paging.PagingSource
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.timschneeberger.shizustore.data.installer.AppInstaller
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.data.work.DownloadWorker
import me.timschneeberger.shizustore.data.work.InstallWorker
import me.timschneeberger.shizustore.util.PathUtil

@Singleton
open class DownloadHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val appInstaller: AppInstaller
) {
    val downloads: Flow<List<Download>> = downloadDao.downloads()

    fun pagedDownloads(): PagingSource<Int, Download> = downloadDao.pagedDownloads()

    suspend fun getDownload(packageName: String): Download? = downloadDao.getDownload(packageName)

    fun hasApk(download: Download): Boolean =
        PathUtil.getApkFile(context, download.packageName, download.versionCode).exists()

    suspend fun enqueue(app: AppEntity, candidate: AppCandidate) {
        stageRow(app, candidate)
        candidate.packageName?.let { DownloadWorker.enqueue(context, it) }
    }

    suspend fun enqueueAndInstall(app: AppEntity, candidate: AppCandidate) {
        val packageName = pipelinePackage(app, candidate)
        stageRow(app, candidate)
        WorkManager.getInstance(context)
            .beginUniqueWork(
                DownloadWorker.uniqueName(packageName),
                ExistingWorkPolicy.KEEP,
                DownloadWorker.request(packageName)
            )
            .then(InstallWorker.request(packageName))
            .enqueue()
    }

    suspend fun stageRow(app: AppEntity, candidate: AppCandidate) {
        val packageName = pipelinePackage(app, candidate)
        val existing = downloadDao.getDownload(packageName)
        if (existing != null && existing.isInFlight) {
            Log.i(TAG, "Not restaging $packageName; it is already ${existing.status}")
            return
        }

        downloadDao.insert(Download.fromCatalog(app, candidate))
    }

    /**
     * The package the install pipeline keys on. Mirrors [Download.fromCatalog]:
     * the candidate's own flavor package wins so the staged row, the download and
     * install workers and the installer all agree on one name.
     */
    private fun pipelinePackage(app: AppEntity, candidate: AppCandidate): String =
        candidate.packageName ?: app.packageName ?: app.slug

    open suspend fun cancel(packageName: String) {
        downloadDao.updateStatusAndError(packageName, DownloadStatus.CANCELLED, null)

        WorkManager.getInstance(context).cancelUniqueWork(DownloadWorker.uniqueName(packageName))

        appInstaller.cancelInstall(packageName)
    }

    open suspend fun retry(packageName: String) {
        val existing = downloadDao.getDownload(packageName)
        if (existing == null) {
            Log.w(TAG, "Not retrying $packageName; it has no download row")
            return
        }

        if (existing.isInFlight) {
            Log.i(TAG, "Not retrying $packageName; it is already ${existing.status}")
            return
        }

        if (existing.canInstallFromDisk(hasApk(existing))) {
            Log.i(TAG, "Retrying $packageName from the apk it already has")
            WorkManager.getInstance(context).enqueueUniqueWork(
                DownloadWorker.uniqueName(packageName),
                ExistingWorkPolicy.KEEP,
                InstallWorker.request(packageName)
            )
            return
        }

        downloadDao.updateStatusAndError(packageName, DownloadStatus.QUEUED, null)
        downloadDao.updateProgress(packageName, 0, 0L, 0L)
        DownloadWorker.enqueue(context, packageName)
    }

    suspend fun cancelAll() {
        downloads.first()
            .filter { it.isInFlight }
            .forEach { cancel(it.packageName) }
    }

    suspend fun clearFinished() {
        downloads.first()
            .filter { it.isFinished }
            .forEach { downloadDao.delete(it.packageName) }
    }

    /** Cancels what is still running before forgetting the lot, so no worker outlives its row. */
    suspend fun clearAll() {
        cancelAll()
        downloadDao.deleteAll()
    }

    suspend fun remove(packageName: String) {
        downloadDao.delete(packageName)
    }

    private val Download.isInFlight: Boolean
        get() = isActive || isInstalling

    private companion object {
        const val TAG = "DownloadHelper"
    }
}
