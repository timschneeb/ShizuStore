/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's RootInstaller (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.installer

import android.content.Context
import android.os.Process
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.timschneeberger.shizustore.data.helper.InstallReporter
import me.timschneeberger.shizustore.data.installer.base.InstallerBase
import me.timschneeberger.shizustore.data.model.InstallError
import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.data.room.entity.Download

@Singleton
open class RootInstaller @Inject constructor(
    @ApplicationContext context: Context,
    downloadDao: DownloadDao,
    installReporter: InstallReporter
) : InstallerBase(context, downloadDao, installReporter) {
    override suspend fun beginInstall(download: Download) {
        val packageName = download.packageName

        runCatching { runInstall(download) }.onFailure { failure ->
            Log.e(TAG, "Root install of $packageName failed unexpectedly", failure)
            postError(
                packageName,
                InstallError.SessionFailure(packageName, failure.localizedMessage)
            )
        }
    }

    private suspend fun runInstall(download: Download) {
        val packageName = download.packageName

        if (!hasRootAccess(useCache = false)) {
            Log.e(TAG, "No root access, refusing to install $packageName")
            postError(packageName, InstallError.RootUnavailable)
            return
        }

        val apkFile = requireApkFile(download) ?: return

        Log.i(TAG, "Received root install request for $packageName")

        val result = shellJob(installCommand(apkFile))
            .to(ArrayList(), ArrayList())
            .exec()

        if (isSuccess(result)) {
            removeFromInstallQueue(packageName)
            onInstallationSuccess(packageName)
        } else {
            postError(packageName, InstallError.SessionFailure(packageName, describe(result)))
        }
    }

    override fun cancelInstall(packageName: String) {
        removeFromInstallQueue(packageName)
    }

    private fun installCommand(apkFile: File): String {
        val userId = Process.myUid() / PER_USER_RANGE
        return "cat ${quote(apkFile.absolutePath)} | pm install -r -t " +
            "-i ${quote(context.packageName)} --user $userId -S ${apkFile.length()}"
    }

    private fun isSuccess(result: Shell.Result): Boolean =
        result.isSuccess && result.out.none { it.trimStart().startsWith(FAILURE_PREFIX) }

    private fun describe(result: Shell.Result): String {
        val stderr = result.err.joinToString(separator = "\n").trim()
        if (stderr.isNotEmpty()) return stderr

        val stdout = result.out.joinToString(separator = "\n").trim()
        if (stdout.isNotEmpty()) return stdout

        return "pm install exited with ${result.code}"
    }

    private fun quote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    companion object {
        private const val TAG = "RootInstaller"

        val installer = Installer.ROOT

        private const val PER_USER_RANGE = 100_000

        private const val FAILURE_PREFIX = "Failure"

        @Volatile
        private var cachedRootAccess: Boolean? = null

        @VisibleForTesting
        internal var rootProbe: () -> Boolean = { Shell.getShell().isRoot }
            set(value) {
                field = value
                cachedRootAccess = null
            }

        @VisibleForTesting
        internal var shellJob: (String) -> Shell.Job = { Shell.cmd(it) }

        suspend fun hasRootAccess(useCache: Boolean = true): Boolean {
            if (useCache) cachedRootAccess?.let { return it }

            return withContext(Dispatchers.IO) {
                val probed = runCatching { rootProbe() }.getOrElse { failure ->
                    Log.w(TAG, "Root probe failed, assuming no root", failure)
                    false
                }
                cachedRootAccess = probed
                probed
            }
        }
    }
}
