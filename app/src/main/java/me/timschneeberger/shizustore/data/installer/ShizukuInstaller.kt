/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's ShizukuInstaller (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageInstallerHidden
import android.content.pm.PackageManager
import android.content.pm.PackageManagerHidden
import android.os.IBinder
import android.os.IInterface
import android.os.Process
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.rikka.tools.refine.Refine
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.helper.InstallReporter
import me.timschneeberger.shizustore.data.installer.base.InstallerBase
import me.timschneeberger.shizustore.data.model.InstallError
import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.data.receiver.InstallerStatusReceiver
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.extensions.isOAndAbove
import me.timschneeberger.shizustore.extensions.isSAndAbove
import me.timschneeberger.shizustore.util.Preferences
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.SystemServiceHelper
import rikka.sui.Sui

@Singleton
open class ShizukuInstaller @Inject constructor(
    @ApplicationContext context: Context,
    downloadDao: DownloadDao,
    installReporter: InstallReporter
) : InstallerBase(context, downloadDao, installReporter) {
    private val stagedSessions = ConcurrentHashMap<String, Int>()

    private fun packageInstaller(installerPackageName: String): PackageInstaller? {
        val userId = Process.myUid() / PER_USER_RANGE
        return when {
            isSAndAbove -> Refine.unsafeCast(
                PackageInstallerHidden(iPackageInstaller, installerPackageName, null, userId)
            )

            isOAndAbove -> Refine.unsafeCast(
                PackageInstallerHidden(iPackageInstaller, installerPackageName, userId)
            )

            else -> null
        }
    }

    override suspend fun beginInstall(download: Download) {
        val packageName = download.packageName

        runCatching { runInstall(download) }.onFailure { failure ->
            Log.e(TAG, "Shizuku install of $packageName failed unexpectedly", failure)
            postError(packageName, typedFailure(packageName, failure))
        }
    }

    private suspend fun runInstall(download: Download) {
        val packageName = download.packageName

        if (!isAvailable(context) || !hasPermission()) {
            Log.e(TAG, "Shizuku is not usable, refusing to install $packageName")
            postError(packageName, InstallError.ShizukuUnavailable)
            return
        }

        val apkFile = requireApkFile(download) ?: return
        val installerPackageName = resolveInstallerPackage()

        Log.i(TAG, "Received shizuku install request for $packageName")

        val (sessionId, session) = runCatching {
            openPrivilegedSession(packageName, installerPackageName)
        }
            .getOrElse { failure ->
                Log.e(TAG, "Could not open a privileged session for $packageName", failure)
                postError(packageName, typedFailure(packageName, failure))
                return
            }

        stagedSessions[packageName] = sessionId

        val pendingIntent = callbackIntent(sessionId, download)

        try {
            Log.i(TAG, "Writing APK to session for $packageName")
            apkFile.inputStream().use { input ->
                session.openWrite(
                    "${packageName}_${apkFile.name}",
                    0,
                    apkFile.length()
                ).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            Log.i(TAG, "Starting install session for $packageName")
            session.commit(pendingIntent.intentSender)
        } catch (exception: Exception) {
            abandonQuietly(session)
            stagedSessions.remove(packageName)
            postError(packageName, typedFailure(packageName, exception))
            return
        }

        stagedSessions.remove(packageName)
        removeFromInstallQueue(packageName)
        closeQuietly(session)
    }

    override fun cancelInstall(packageName: String) {
        stagedSessions.remove(packageName)?.let { sessionId ->
            Log.i(TAG, "Abandoning session $sessionId for $packageName")
            abandonSessionQuietly(sessionId)?.let {
                Log.w(TAG, "Failed to abandon session $sessionId", it)
            }
        }

        removeFromInstallQueue(packageName)
    }

    private fun openPrivilegedSession(
        packageName: String,
        installerPackageName: String
    ): Pair<Int, PackageInstaller.Session> {
        val params = SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
        }

        val hiddenParams = Refine.unsafeCast<PackageInstallerHidden.SessionParamsHidden>(params)
        hiddenParams.installFlags =
            hiddenParams.installFlags or PackageManagerHidden.INSTALL_REPLACE_EXISTING

        val installer = checkNotNull(packageInstaller(installerPackageName)) {
            "Shizuku installs need API 26 or newer"
        }

        val sessionId = installer.createSession(params)
        val iSession = IPackageInstallerSession.Stub.asInterface(
            iPackageInstaller.openSession(sessionId).asShizukuBinder()
        )

        return sessionId to Refine.unsafeCast<PackageInstaller.Session>(
            PackageInstallerHidden.SessionHidden(iSession)
        )
    }

    private suspend fun resolveInstallerPackage(): String = effectiveInstallerSourcePackage(
        fallbackPackage = context.packageName,
        customSourceEnabled = Preferences.readBoolean(
            context,
            Preferences.PREFERENCE_INSTALLER_CUSTOM_SOURCE
        ),
        customSourcePackage = Preferences.readString(
            context,
            Preferences.PREFERENCE_INSTALLER_CUSTOM_SOURCE_PACKAGE,
            Preferences.DEFAULT_INSTALLER_SOURCE_PACKAGE
        )
    )

    private fun callbackIntent(sessionId: Int, download: Download): PendingIntent {
        val callBackIntent = Intent(context, InstallerStatusReceiver::class.java).apply {
            action = InstallerStatusReceiver.ACTION_INSTALL_STATUS
            setPackage(context.packageName)
            putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionId)
            putExtra(InstallerStatusReceiver.EXTRA_PACKAGE_NAME, download.packageName)
            putExtra(InstallerStatusReceiver.EXTRA_VERSION_CODE, download.versionCode)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        return PendingIntent.getBroadcast(
            context,
            sessionId,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    private fun typedFailure(packageName: String, failure: Throwable): InstallError =
        if (hasPermission()) {
            InstallError.SessionFailure(packageName, failure.localizedMessage)
        } else {
            InstallError.ShizukuUnavailable
        }

    private fun hasPermission(): Boolean = Companion.hasPermission()

    companion object {
        private const val TAG = "ShizukuInstaller"

        private const val PER_USER_RANGE = 100_000

        private fun IBinder.wrap() = ShizukuBinderWrapper(this)

        private fun IInterface.asShizukuBinder() = this.asBinder().wrap()

        private val iPackageManager: IPackageManager by lazy {
            IPackageManager.Stub.asInterface(SystemServiceHelper.getSystemService("package").wrap())
        }

        private val iPackageInstaller: IPackageInstaller by lazy {
            IPackageInstaller.Stub.asInterface(iPackageManager.packageInstaller.asShizukuBinder())
        }

        fun abandonSessionQuietly(sessionId: Int): Throwable? =
            runCatching { iPackageInstaller.abandonSession(sessionId) }.exceptionOrNull()

        private const val PERMISSION_REQUEST_CODE = 9_000

        private val DEFAULT_PERMISSION_PROBE: () -> Boolean = {
            Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }

        private val permissionProbe: () -> Boolean = DEFAULT_PERMISSION_PROBE

        fun hasPermission(): Boolean = runCatching { permissionProbe() }.getOrElse {
            Log.w(TAG, "Shizuku permission check failed", it)
            false
        }

        fun requestPermissionIfNeeded() = permissionRequester()

        private val DEFAULT_PERMISSION_REQUESTER: () -> Unit = {
            if (isRunning() && !hasPermission()) {
                Log.i(TAG, "Shizuku is running but not permitted; asking for the grant")
                runCatching { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) }.onFailure {
                    Log.w(TAG, "Could not ask Shizuku for permission", it)
                }
            }
        }

        private val permissionRequester: () -> Unit = DEFAULT_PERMISSION_REQUESTER

        fun isRunning(): Boolean = runCatching { Shizuku.pingBinder() }
            .getOrElse { false }

        // Detects a Shizuku manager by the permissions it declares instead of by package name:
        // forks ship under different packages (or hide themselves from package enumeration), while
        // permission names are shared and live in a global namespace. Stock first so installs that
        // declare both keep resolving to the same package as before.
        internal const val SHIZUKU_PLUS_PERMISSION = "af.shizuku.plus.permission.API_V23"

        internal val MANAGER_PERMISSIONS = listOf(
            ShizukuProvider.PERMISSION,
            SHIZUKU_PLUS_PERMISSION
        )

        /** Packages declaring a Shizuku manager permission, stock first; empty when none is installed. */
        fun managerPackages(context: Context): List<String> =
            MANAGER_PERMISSIONS.mapNotNull { resolvePermissionOwner(context, it) }.distinct()

        private fun resolvePermissionOwner(context: Context, permission: String): String? = try {
            context.packageManager.getPermissionInfo(permission, 0).packageName
                ?.takeUnless { it.isBlank() }
        } catch (exception: PackageManager.NameNotFoundException) {
            null
        } catch (exception: Exception) {
            Log.w(TAG, "Could not resolve the owner of $permission", exception)
            null
        }

        val installer = Installer.SHIZUKU

        private val DEFAULT_AVAILABILITY_PROBE: (Context) -> Boolean = { context ->
            isOAndAbove && (managerPackages(context).isNotEmpty() || Sui.isSui())
        }

        private val availabilityProbe: (Context) -> Boolean = DEFAULT_AVAILABILITY_PROBE

        fun isAvailable(context: Context): Boolean =
            runCatching { availabilityProbe(context) }.getOrElse { failure ->
                Log.w(TAG, "Shizuku availability probe failed, assuming absent", failure)
                false
            }
    }
}

// Top level so tests can exercise it without loading the installer's hidden-API class references.
internal fun effectiveInstallerSourcePackage(
    fallbackPackage: String,
    customSourceEnabled: Boolean,
    customSourcePackage: String
): String = if (customSourceEnabled && customSourcePackage.isNotBlank()) {
    customSourcePackage
} else {
    fallbackPackage
}
