/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's SessionInstaller (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.installer

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageInstaller.PACKAGE_SOURCE_STORE
import android.content.pm.PackageInstaller.SessionParams
import android.content.pm.PackageManager
import android.os.Process
import android.util.Log
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.timschneeberger.shizustore.data.helper.InstallReporter
import me.timschneeberger.shizustore.data.installer.base.InstallerBase
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.model.InstallError
import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.data.model.SessionInfo
import me.timschneeberger.shizustore.data.receiver.InstallerStatusReceiver
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.extensions.isOAndAbove
import me.timschneeberger.shizustore.extensions.isQAndAbove
import me.timschneeberger.shizustore.extensions.isSAndAbove
import me.timschneeberger.shizustore.extensions.isTAndAbove
import me.timschneeberger.shizustore.extensions.isUAndAbove
import me.timschneeberger.shizustore.extensions.runOnUiThread
import me.timschneeberger.shizustore.util.isolatedIoScope

@Singleton
open class SessionInstaller @Inject constructor(
    @ApplicationContext context: Context,
    private val downloadDao: DownloadDao,
    installReporter: InstallReporter
) : InstallerBase(context, downloadDao, installReporter) {
    private val packageInstaller = context.packageManager.packageInstaller

    private val enqueuedSessions = ConcurrentHashMap<Int, SessionInfo>()

    private val committedSessions: MutableSet<Int> = ConcurrentHashMap.newKeySet()

    private val scope = isolatedIoScope(TAG)

    private val callback = object : PackageInstaller.SessionCallback() {
        override fun onCreated(sessionId: Int) {}

        override fun onBadgingChanged(sessionId: Int) {}

        override fun onActiveChanged(sessionId: Int, active: Boolean) {}

        override fun onProgressChanged(sessionId: Int, progress: Float) {
            val packageName = enqueuedSessions[sessionId]?.packageName ?: return

            if (progress > 0.0) {
                scope.launch {
                    downloadDao.updateProgress(
                        packageName = packageName,
                        progress = (progress * 100).toInt(),
                        speed = 0L,
                        timeRemaining = 0L
                    )
                }
            }
        }

        override fun onFinished(sessionId: Int, success: Boolean) {
            forget(sessionId)
        }
    }

    init {
        runOnUiThread { packageInstaller.registerSessionCallback(callback) }
    }

    override suspend fun beginInstall(download: Download) {
        val packageName = download.packageName
        val staged = enqueuedSessions.values.firstOrNull {
            it.packageName == packageName && it.sessionId !in committedSessions
        }

        if (staged != null) {
            if (staged.versionCode == download.versionCode) {
                Log.i(TAG, "$packageName ${download.versionCode} is already staged, committing")
                commitInstall(staged)
                return
            }
            Log.i(TAG, "Discarding staged session for superseded ${staged.versionCode}")
            abandon(staged.sessionId)
        }

        val apkFile = requireApkFile(download) ?: return

        Log.i(TAG, "Received session install request for $packageName")
        val sessionInfo = stageInstall(download, apkFile) ?: return

        enqueuedSessions[sessionInfo.sessionId] = sessionInfo
        commitInstall(sessionInfo)
    }

    override fun cancelInstall(packageName: String) {
        enqueuedSessions.values
            .filter { it.packageName == packageName }
            .forEach { sessionInfo ->
                Log.i(TAG, "Abandoning session ${sessionInfo.sessionId} for $packageName")
                abandon(sessionInfo.sessionId)
            }

        removeFromInstallQueue(packageName)
    }

    suspend fun abandonOrphanedSessions() = withContext(Dispatchers.IO) {
        val tracked = enqueuedSessions.keys + committedSessions

        packageInstaller.mySessions
            .filterNot { couldStillBeLive(it) }
            .filterNot { it.sessionId in tracked }
            .filterNot { session ->
                session.appPackageName?.let { InstallerBase.wasDispatchedInThisProcess(it) } == true
            }
            .forEach { session ->
                Log.i(
                    TAG,
                    "Abandoning orphaned session ${session.sessionId} " +
                        "(${session.appPackageName})"
                )
                abandonSessionQuietly(session.sessionId)
                    ?.let { reclaimThroughShizuku(session.sessionId, it) }
                    ?.let {
                        Log.w(TAG, "Could not abandon orphaned session ${session.sessionId}", it)
                    }
            }
    }

    private fun reclaimThroughShizuku(sessionId: Int, failure: Throwable): Throwable? {
        if (failure !is SecurityException) return failure
        if (!ShizukuInstaller.hasPermission()) return failure

        Log.i(TAG, "Retrying orphaned session $sessionId through Shizuku's binder")
        return abandonThroughShizuku(sessionId)
    }

    private fun couldStillBeLive(session: PackageInstaller.SessionInfo): Boolean =
        if (isQAndAbove) {
            session.isCommitted &&
                System.currentTimeMillis() - session.updatedMillis < COMMITTED_SESSION_MAX_AGE
        } else {
            true
        }

    suspend fun downloadStatus(packageName: String): DownloadStatus? =
        downloadDao.getDownload(packageName)?.status

    suspend fun awaitingConfirmation(packageName: String) =
        downloadDao.updateStatus(packageName, DownloadStatus.AWAITING_CONFIRMATION)

    suspend fun displayName(packageName: String): String =
        downloadDao.getDownload(packageName)?.displayName ?: packageName

    fun onSessionInstalled(packageName: String) {
        scope.launch {
            downloadDao.updateProgress(
                packageName = packageName,
                progress = 100,
                speed = 0L,
                timeRemaining = 0L
            )
        }
        onInstallationSuccess(packageName)
    }

    private fun stageInstall(download: Download, apkFile: File): SessionInfo? {
        val packageName = download.packageName

        val sessionId = try {
            packageInstaller.createSession(buildSessionParams(packageName, apkFile.length()))
        } catch (exception: Exception) {
            postError(
                packageName,
                if (exception is IOException) {
                    InstallError.StorageFull(packageName)
                } else {
                    InstallError.SessionFailure(packageName, exception.localizedMessage)
                }
            )
            return null
        }

        return try {
            Log.i(TAG, "Writing APK to session for $packageName")
            packageInstaller.openSession(sessionId).use { session ->
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
            }
            SessionInfo(sessionId, packageName, download.versionCode, download.displayName)
        } catch (exception: Exception) {
            abandon(sessionId)
            postError(
                packageName,
                InstallError.SessionFailure(packageName, exception.localizedMessage)
            )
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun buildSessionParams(packageName: String, totalSize: Long = 0L): SessionParams =
        SessionParams(SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            if (totalSize > 0) setSize(totalSize)
            setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO)
            setOriginatingUid(Process.myUid())
            if (isOAndAbove) {
                setInstallReason(PackageManager.INSTALL_REASON_USER)
            }
            if (isSAndAbove) {
                setRequireUserAction(SessionParams.USER_ACTION_NOT_REQUIRED)
            }
            if (isTAndAbove) {
                setPackageSource(PACKAGE_SOURCE_STORE)
            }
            if (isUAndAbove) {
                setInstallerPackageName(context.packageName)
                setRequestUpdateOwnership(true)
                setApplicationEnabledSettingPersistent()
            }
        }

    private fun commitInstall(sessionInfo: SessionInfo) {
        val packageName = sessionInfo.packageName

        try {
            Log.i(TAG, "Starting install session for $packageName")

            val existingSessionInfo = packageInstaller.getSessionInfo(sessionInfo.sessionId)
            if (existingSessionInfo == null) {
                Log.e(TAG, "Session ${sessionInfo.sessionId} is no longer valid")
                postError(
                    packageName,
                    InstallError.SessionFailure(
                        packageName,
                        "session ${sessionInfo.sessionId} is no longer valid"
                    )
                )
                return
            }

            commitSession(sessionInfo)
        } catch (exception: Exception) {
            Log.e(TAG, "Error committing session: ${exception.message}")
            postError(
                packageName,
                InstallError.SessionFailure(packageName, exception.localizedMessage)
            )
        }
    }

    private fun commitSession(sessionInfo: SessionInfo) {
        val session = packageInstaller.openSession(sessionInfo.sessionId)

        try {
            session.commit(getCallBackIntent(sessionInfo).intentSender)
        } catch (exception: Exception) {
            closeQuietly(session)
            throw exception
        }

        committedSessions.add(sessionInfo.sessionId)
        removeFromInstallQueue(sessionInfo.packageName)
        closeQuietly(session)
    }

    @VisibleForTesting
    internal open fun abandonSessionQuietly(sessionId: Int): Throwable? =
        runCatching { packageInstaller.abandonSession(sessionId) }.exceptionOrNull()

    @VisibleForTesting
    internal open fun abandonThroughShizuku(sessionId: Int): Throwable? =
        ShizukuInstaller.abandonSessionQuietly(sessionId)

    private fun abandon(sessionId: Int) {
        abandonSessionQuietly(sessionId)
        forget(sessionId)
    }

    private fun forget(sessionId: Int) {
        enqueuedSessions.remove(sessionId)
        committedSessions.remove(sessionId)
    }

    private fun getCallBackIntent(sessionInfo: SessionInfo): PendingIntent {
        val callBackIntent = Intent(context, InstallerStatusReceiver::class.java).apply {
            action = InstallerStatusReceiver.ACTION_INSTALL_STATUS
            setPackage(context.packageName)
            putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionInfo.sessionId)
            putExtra(InstallerStatusReceiver.EXTRA_PACKAGE_NAME, sessionInfo.packageName)
            putExtra(InstallerStatusReceiver.EXTRA_VERSION_CODE, sessionInfo.versionCode)
            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        }

        return PendingIntent.getBroadcast(
            context,
            sessionInfo.sessionId,
            callBackIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    companion object {
        private const val TAG = "SessionInstaller"

        private val COMMITTED_SESSION_MAX_AGE = TimeUnit.HOURS.toMillis(24)

        val installer = Installer.SESSION
    }
}
