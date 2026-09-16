/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's InstallerBase (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.installer.base

import android.content.Context
import android.content.pm.PackageInstaller
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.compose.stringRes
import me.timschneeberger.shizustore.data.helper.InstallReporter
import me.timschneeberger.shizustore.data.model.DownloadFailure
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.model.InstallError
import me.timschneeberger.shizustore.data.model.signaturesMatch
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.util.CertUtil
import me.timschneeberger.shizustore.util.NotificationUtil
import me.timschneeberger.shizustore.util.PathUtil
import me.timschneeberger.shizustore.util.isolate
import me.timschneeberger.shizustore.util.isolatedIoScope

abstract class InstallerBase(
    protected val context: Context,
    private val downloadDao: DownloadDao,
    private val installReporter: InstallReporter
) : IInstaller {
    final override fun install(download: Download) {
        synchronized(queue) { queue.add(download.packageName) }
        synchronized(dispatched) { dispatched.add(download.packageName) }
        scope.launch {
            val packageName = download.packageName

            if (refuseOnSignerMismatch(download)) return@launch

            val status = runCatching { downloadDao.getDownload(packageName)?.status }.getOrNull()
            if (status == DownloadStatus.CANCELLED) {
                Log.i(TAG, "$packageName was cancelled before its install began; standing down")
                removeFromInstallQueue(packageName)
                return@launch
            }

            val flipped = isolate(TAG, "flip $packageName to INSTALLING") {
                downloadDao.updateStatusAndError(packageName, DownloadStatus.INSTALLING, null)
            }

            if (!flipped) {
                removeFromInstallQueue(packageName)
                return@launch
            }

            // Swap the still-showing "downloaded" notification for an installing row.
            isolate(TAG, "notify installing $packageName") {
                NotificationUtil.notifyApp(
                    context,
                    packageName,
                    NotificationUtil.installingNotification(
                        context,
                        packageName,
                        download.displayName
                    )
                )
            }

            if (!isolate(TAG, "install $packageName") { beginInstall(download) }) {
                postError(packageName, InstallError.SessionFailure(packageName, "unexpected error"))
            }
        }
    }

    private fun refuseOnSignerMismatch(download: Download): Boolean {
        val installed = CertUtil.getSigningFingerprints(context, download.packageName)
        if (installed.isEmpty()) return false

        if (signaturesMatch(installed, download.signer, download.sigMd5)) return false

        Log.e(
            TAG,
            "Refusing to install ${download.packageName}: its signing set is not the " +
                "installed copy's"
        )
        postError(download.packageName, InstallError.SignerMismatch(download.packageName))
        return true
    }

    protected abstract suspend fun beginInstall(download: Download)

    override fun removeFromInstallQueue(packageName: String) {
        synchronized(queue) { queue.remove(packageName) }
    }

    protected fun getApkFile(download: Download): File =
        PathUtil.getApkFile(context, download.packageName, download.versionCode)

    /** Returns the staged APK, or reports [InstallError.ApkMissing] and returns null. */
    protected fun requireApkFile(download: Download): File? {
        val apk = getApkFile(download)
        if (apk.exists()) return apk
        postError(download.packageName, InstallError.ApkMissing(download.packageName))
        return null
    }

    protected fun getUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileProvider", file)

    fun onInstallationSuccess(packageName: String) {
        scope.launch {
            val row = downloadDao.getDownload(packageName)
            val displayName = row?.displayName ?: packageName
            downloadDao.updateStatusAndError(packageName, DownloadStatus.INSTALLED, null)

            row?.let { installReporter.reportInstalled(packageName, it.versionCode) }

            NotificationUtil.notifyApp(
                context,
                packageName,
                NotificationUtil.installedNotification(context, packageName, displayName)
            )

            isolate(TAG, "reclaim $packageName's installed apk") { reclaimApk(packageName) }
        }
    }

    private suspend fun reclaimApk(packageName: String) {
        val row = downloadDao.getDownload(packageName) ?: return
        if (row.status != DownloadStatus.INSTALLED) {
            Log.i(TAG, "Not reclaiming $packageName's apk: the row is ${row.status}")
            return
        }

        val apk = getApkFile(row)
        if (!apk.exists()) return

        val bytes = apk.length()
        if (apk.delete()) {
            Log.i(TAG, "Reclaimed $bytes bytes from ${apk.name}")
        } else {
            Log.w(TAG, "Could not delete ${apk.name} after installing $packageName")
        }
    }

    fun postError(packageName: String, error: InstallError) {
        Log.e(TAG, "Install failed for $packageName: $error")
        scope.launch {
            val status = runCatching { downloadDao.getDownload(packageName)?.status }.getOrNull()
            if (status == DownloadStatus.CANCELLED) {
                Log.i(TAG, "$packageName is CANCELLED; not relabelling it FAILED ($error)")
                return@launch
            }

            downloadDao.updateStatusAndError(
                packageName,
                DownloadStatus.FAILED,
                DownloadFailure.of(error)
            )

            val displayName = downloadDao.getDownload(packageName)?.displayName ?: packageName
            NotificationUtil.notifyApp(
                context,
                packageName,
                NotificationUtil.installFailedNotification(
                    context = context,
                    packageName = packageName,
                    displayName = displayName,
                    reason = context.getString(error.stringRes())
                )
            )
        }
        onInstallFailed(packageName, error)
    }

    protected open fun onInstallFailed(packageName: String, error: InstallError) {
        cancelInstall(packageName)
    }

    protected fun closeQuietly(session: PackageInstaller.Session) {
        runCatching { session.close() }
            .onFailure { Log.w(TAG, "Failed to close session handle", it) }
    }

    protected fun abandonQuietly(session: PackageInstaller.Session) {
        runCatching { session.abandon() }
            .onFailure { Log.w(TAG, "Failed to abandon session", it) }
    }

    companion object {
        private const val TAG = "InstallerBase"

        private val queue: MutableSet<String> = mutableSetOf()

        private val dispatched: MutableSet<String> = mutableSetOf()

        fun wasDispatchedInThisProcess(packageName: String): Boolean =
            synchronized(dispatched) { dispatched.contains(packageName) }

        private val scope: CoroutineScope = isolatedIoScope(TAG)
    }
}
