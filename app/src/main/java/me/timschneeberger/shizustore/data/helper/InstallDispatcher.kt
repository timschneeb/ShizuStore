/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.installer.AppInstaller
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.model.InstallDispatch
import me.timschneeberger.shizustore.data.room.entity.Download

@Singleton
class InstallDispatcher @Inject constructor(
    private val downloadHelper: DownloadHelper,
    private val appInstaller: AppInstaller
) {
    suspend fun dispatch(packageName: String): InstallDispatch {
        val row = downloadHelper.getDownload(packageName) ?: return InstallDispatch.Refused(null)
        return appInstaller.install(row)
    }

    fun canInstallFromDisk(row: Download): Boolean =
        row.canInstallFromDisk(downloadHelper.hasApk(row))
}

/** A row whose APK is still on disk can be installed again without downloading. */
fun Download.canInstallFromDisk(hasApk: Boolean): Boolean {
    val reusable = when (status) {
        DownloadStatus.COMPLETED -> true
        DownloadStatus.FAILED -> error?.canRetryWithoutDownloading == true
        else -> false
    }

    return reusable && hasApk
}
