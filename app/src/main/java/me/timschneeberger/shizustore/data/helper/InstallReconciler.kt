/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import android.content.Context
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.timschneeberger.shizustore.data.installer.SessionInstaller
import me.timschneeberger.shizustore.data.installer.base.InstallerBase
import me.timschneeberger.shizustore.data.model.DownloadFailure
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.util.NotificationUtil
import me.timschneeberger.shizustore.util.PathUtil
import me.timschneeberger.shizustore.util.isolate

@Singleton
open class InstallReconciler @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val sessionInstaller: SessionInstaller
) {
    open suspend fun reconcileOnLaunch() {
        isolate(TAG, "reconcile stranded installs") { reconcileStrandedInstalls() }
        isolate(TAG, "sweep orphaned apks") { sweepOrphanedApks() }
        isolate(TAG, "abandon orphaned sessions") { sessionInstaller.abandonOrphanedSessions() }
    }

    open suspend fun onPackageInstalled(packageName: String) = withContext(Dispatchers.IO) {
        val download = downloadDao.getDownload(packageName) ?: return@withContext
        if (!download.isInstalling) return@withContext

        val installed = installedVersionCode(packageName)
        if (installed == download.versionCode) {
            Log.i(TAG, "$packageName installed at $installed; settling the row as INSTALLED")
            settle(packageName, DownloadStatus.INSTALLED, null, download.status)
        } else {
            Log.i(
                TAG,
                "$packageName was installed at $installed but the row is installing " +
                    "${download.versionCode}; leaving it alone"
            )
        }
    }

    open suspend fun reconcileStrandedInstalls() = withContext(Dispatchers.IO) {
        val stranded = downloadDao.downloads().first().filter { it.isInstalling }

        stranded.forEach { download ->
            if (InstallerBase.wasDispatchedInThisProcess(download.packageName)) {
                Log.i(
                    TAG,
                    "${download.packageName} is ${download.status} from this process; " +
                        "not settling it"
                )
                return@forEach
            }

            val installed = installedVersionCode(download.packageName)
            val landed = installed == download.versionCode
            val settled = if (landed) DownloadStatus.INSTALLED else DownloadStatus.FAILED

            Log.i(
                TAG,
                "${download.packageName} was stranded at ${download.status} " +
                    "(row ${download.versionCode}, installed $installed); settling as $settled"
            )
            settle(
                packageName = download.packageName,
                status = settled,
                error = if (landed) null else DownloadFailure.INSTALL_INTERRUPTED,
                expected = download.status
            )
        }
    }

    open suspend fun sweepOrphanedApks() = withContext(Dispatchers.IO) {
        val claimed = downloadDao.downloads().first()
            .filterNot { it.status == DownloadStatus.INSTALLED }
            .flatMapTo(mutableSetOf()) { row ->
                val names = mutableListOf(
                    PathUtil.getApkFile(context, row.packageName, row.versionCode).name
                )
                if (!row.archiveEntry.isNullOrBlank()) {
                    names.add(
                        PathUtil.getArchiveFile(context, row.packageName, row.versionCode).name
                    )
                }
                names
            }

        PathUtil.getApkDir(context).listFiles().orEmpty()
            .filterNot { it.name in claimed }
            .forEach { file ->
                val bytes = file.length()
                runCatching { file.delete() }
                    .onSuccess { deleted ->
                        if (deleted) {
                            Log.i(TAG, "Reclaimed $bytes bytes from the orphaned ${file.name}")
                        } else {
                            Log.w(TAG, "Could not delete the orphaned ${file.name}")
                        }
                    }
                    .onFailure { Log.w(TAG, "Could not delete the orphaned ${file.name}", it) }
            }
    }

    private suspend fun settle(
        packageName: String,
        status: DownloadStatus,
        error: DownloadFailure?,
        expected: DownloadStatus
    ) {
        val updated = downloadDao.updateStatusAndErrorIf(
            packageName = packageName,
            status = status,
            error = error,
            expected = expected
        )

        if (updated == 0) {
            Log.i(TAG, "$packageName left $expected before it could be settled as $status")
            return
        }

        NotificationUtil.clearAppNotification(context, packageName)
    }

    private fun installedVersionCode(packageName: String): Long? = runCatching {
        PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(packageName, 0)
        )
    }.getOrNull()

    private companion object {
        const val TAG = "InstallReconciler"
    }
}
