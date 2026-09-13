/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's AppInstaller (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.installer

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.installer.base.IInstaller
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.model.InstallDispatch
import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.util.Preferences

@Singleton
open class AppInstaller @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val sessionInstaller: SessionInstaller,
    private val nativeInstaller: NativeInstaller,
    private val rootInstaller: RootInstaller,
    private val shizukuInstaller: ShizukuInstaller
) {
    private val installers: List<IInstaller>
        get() = listOf(sessionInstaller, nativeInstaller, rootInstaller, shizukuInstaller)

    open suspend fun install(download: Download): InstallDispatch {
        val packageName = download.packageName
        val status = downloadDao.getDownload(packageName)?.status

        if (status == null || status !in INSTALLABLE_STATUSES) {
            Log.w(TAG, "Refusing to install $packageName: the stored row is $status")
            return InstallDispatch.Refused(status)
        }

        val installer = getCurrentInstaller(context)

        if (installer == Installer.SHIZUKU) ShizukuInstaller.requestPermissionIfNeeded()

        backendFor(installer).install(download)
        return InstallDispatch.Started
    }

    open fun cancelInstall(packageName: String) {
        installers.forEach { it.cancelInstall(packageName) }
    }

    private fun backendFor(installer: Installer): IInstaller = when (installer) {
        Installer.SESSION -> sessionInstaller
        Installer.NATIVE -> nativeInstaller
        Installer.ROOT -> rootInstaller
        Installer.SHIZUKU -> shizukuInstaller
    }

    companion object {
        private const val TAG = "AppInstaller"

        val INSTALLABLE_STATUSES: Set<DownloadStatus> = setOf(
            DownloadStatus.COMPLETED,
            DownloadStatus.FAILED,
            DownloadStatus.INSTALLED
        )

        suspend fun availableInstallers(context: Context): List<Installer> =
            Installer.entries.filter { isAvailable(it, context) }

        suspend fun isAvailable(selected: Installer, context: Context): Boolean = when (selected) {
            Installer.SESSION, Installer.NATIVE -> true
            Installer.ROOT -> RootInstaller.hasRootAccess()
            Installer.SHIZUKU -> ShizukuInstaller.isAvailable(context)
        }

        suspend fun getCurrentInstaller(context: Context): Installer {
            val stored = Preferences.readInteger(
                context,
                Preferences.PREFERENCE_INSTALLER_ID,
                Installer.SESSION.ordinal
            )

            val selected = Installer.entries.getOrNull(stored)
            if (selected == null) {
                Log.w(TAG, "Stored installer ordinal $stored is out of range, using SESSION")
                return Installer.SESSION
            }

            if (!isAvailable(selected, context)) {
                Log.i(TAG, "$selected is unavailable on this device, falling back to SESSION")
                return Installer.SESSION
            }

            return selected
        }
    }
}
