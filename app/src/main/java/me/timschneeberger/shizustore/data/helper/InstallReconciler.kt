/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
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
    private val sessionInstaller: SessionInstaller,
    private val installReporter: InstallReporter
) {
    open suspend fun reconcileOnLaunch() {
        isolate(TAG, "reconcile stranded installs") { reconcileStrandedInstalls() }
        isolate(TAG, "sweep orphaned apks") { sweepOrphanedApks() }
        isolate(TAG, "abandon orphaned sessions") { sessionInstaller.abandonOrphanedSessions() }
    }

    /**
     * Foreground re-check: OEM freezers can drop the session callback broadcast
     * while the app is backgrounded, so settle stranded rows whenever the UI
     * comes back instead of waiting for the next process start.
     */
    open suspend fun reconcileOnForeground() {
        isolate(TAG, "reconcile stranded installs") { reconcileStrandedInstalls() }
    }

    open suspend fun onPackageInstalled(packageName: String) = withContext(Dispatchers.IO) {
        val download = downloadDao.getDownload(packageName) ?: return@withContext
        if (!download.isInstalling) return@withContext

        val installed = installedVersionCode(packageName)
        if (installed == download.versionCode) {
            Log.i(TAG, "$packageName installed at $installed; settling the row as INSTALLED")
            settle(
                packageName,
                DownloadStatus.INSTALLED,
                null,
                download.status,
                download.versionCode
            )
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
            val installed = installedVersionCode(download.packageName)
            val outcome = strandedInstallOutcome(
                rowVersionCode = download.versionCode,
                installedVersionCode = installed,
                dispatchedInThisProcess = InstallerBase.wasDispatchedInThisProcess(
                    download.packageName
                )
            )

            if (outcome == null) {
                Log.i(
                    TAG,
                    "${download.packageName} is ${download.status} from this process; " +
                        "not settling it"
                )
                return@forEach
            }

            Log.i(
                TAG,
                "${download.packageName} was stranded at ${download.status} " +
                    "(row ${download.versionCode}, installed $installed); settling as $outcome"
            )
            settle(
                packageName = download.packageName,
                status = outcome,
                error = if (outcome == DownloadStatus.INSTALLED) {
                    null
                } else {
                    DownloadFailure.INSTALL_INTERRUPTED
                },
                expected = download.status,
                versionCode = download.versionCode
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
        expected: DownloadStatus,
        versionCode: Long
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

        // Native installs bypass the installer callbacks, so report success here too;
        // the reporter's once-guard dedupes this against onInstallationSuccess.
        if (status == DownloadStatus.INSTALLED) {
            installReporter.reportInstalled(packageName, versionCode)
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

/**
 * Outcome for an installing row found by reconciliation. The row settles as
 * INSTALLED when the device already carries its version, even when this process
 * dispatched it: the landing proof beats the in-flight guard. An unlanded row
 * dispatched by this process may still be in flight and is left alone; anything
 * else is a failed install.
 */
internal fun strandedInstallOutcome(
    rowVersionCode: Long,
    installedVersionCode: Long?,
    dispatchedInThisProcess: Boolean
): DownloadStatus? = when {
    installedVersionCode == rowVersionCode -> DownloadStatus.INSTALLED
    dispatchedInThisProcess -> null
    else -> DownloadStatus.FAILED
}
