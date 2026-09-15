/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's NativeInstaller (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.installer

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.helper.InstallReporter
import me.timschneeberger.shizustore.data.installer.base.InstallerBase
import me.timschneeberger.shizustore.data.model.InstallError
import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.data.room.entity.Download

@Singleton
open class NativeInstaller @Inject constructor(
    @ApplicationContext context: Context,
    downloadDao: DownloadDao,
    installReporter: InstallReporter
) : InstallerBase(context, downloadDao, installReporter) {
    override suspend fun beginInstall(download: Download) {
        val packageName = download.packageName

        val apkFile = getApkFile(download)
        if (!apkFile.exists()) {
            postError(packageName, InstallError.ApkMissing(packageName))
            return
        }

        Log.i(TAG, "Received native install request for $packageName")

        runCatching {
            @Suppress("DEPRECATION")
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                data = getUri(apkFile)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            }
            context.startActivity(intent)
        }.fold(
            onSuccess = {
                removeFromInstallQueue(packageName)
            },
            onFailure = { failure ->
                Log.e(TAG, "Failed to hand $packageName to the system installer", failure)
                postError(
                    packageName,
                    InstallError.SessionFailure(
                        packageName,
                        failure.localizedMessage ?: "no package installer activity"
                    )
                )
            }
        )
    }

    override fun cancelInstall(packageName: String) {
        removeFromInstallQueue(packageName)
    }

    override fun onInstallFailed(packageName: String, error: InstallError) {
        cancelInstall(packageName)
    }

    companion object {
        private const val TAG = "NativeInstaller"

        val installer = Installer.NATIVE
    }
}
