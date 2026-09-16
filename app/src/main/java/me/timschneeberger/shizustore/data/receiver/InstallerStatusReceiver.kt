/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's BaseInstallerStatusReceiver (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.core.content.IntentCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.installer.SessionInstaller
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.model.InstallError
import me.timschneeberger.shizustore.util.NotificationUtil
import me.timschneeberger.shizustore.util.isolatedIoScope

@AndroidEntryPoint
class InstallerStatusReceiver : BroadcastReceiver() {
    @Inject
    lateinit var sessionInstaller: SessionInstaller

    private val scope = isolatedIoScope(TAG)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_STATUS) return

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
        val versionCode = intent.getLongExtra(EXTRA_VERSION_CODE, -1)
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1)
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)

        Log.i(TAG, "$packageName ($versionCode) sessionId=$sessionId status=$status: $message")

        val pendingResult = goAsync()
        scope.launch {
            try {
                when (status) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        promptUser(context, intent, packageName)?.let { reason ->
                            fail(packageName, InstallError.SessionFailure(packageName, reason))
                        }
                    }

                    PackageInstaller.STATUS_SUCCESS -> {
                        NotificationUtil.clearAppNotification(context, packageName)
                        sessionInstaller.removeFromInstallQueue(packageName)
                        sessionInstaller.onSessionInstalled(packageName)
                    }

                    else -> {
                        NotificationUtil.clearAppNotification(context, packageName)
                        fail(
                            packageName,
                            InstallError.fromSessionStatus(status, packageName, message)
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun fail(packageName: String, error: InstallError) {
        if (sessionInstaller.downloadStatus(packageName) == DownloadStatus.CANCELLED) {
            Log.i(TAG, "$packageName is already CANCELLED; not overwriting it with FAILED ($error)")
            sessionInstaller.cancelInstall(packageName)
            return
        }

        sessionInstaller.removeFromInstallQueue(packageName)
        sessionInstaller.postError(packageName, error)
    }

    private suspend fun promptUser(context: Context, intent: Intent, packageName: String): String? {
        val launchIntent = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_INTENT,
            Intent::class.java
        )

        if (launchIntent == null) {
            Log.w(TAG, "No confirmation intent in the installation request")
            return "no confirmation intent in the installation request"
        }

        launchIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
        launchIntent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        sessionInstaller.awaitingConfirmation(packageName)
        NotificationUtil.notify(
            context,
            NotificationUtil.notificationId(packageName),
            NotificationUtil.confirmInstallNotification(
                context = context,
                packageName = packageName,
                displayName = sessionInstaller.displayName(packageName),
                confirmIntent = launchIntent
            )
        )

        runCatching { context.startActivity(launchIntent) }
            .onFailure { Log.i(TAG, "Confirmation stays in the shade: ${it.message}") }

        return null
    }

    companion object {
        private const val TAG = "InstallerStatusReceiver"

        const val ACTION_INSTALL_STATUS =
            "me.timschneeberger.shizustore.data.receiver.InstallerStatusReceiver.INSTALL_STATUS"

        const val EXTRA_PACKAGE_NAME =
            "me.timschneeberger.shizustore.data.receiver.InstallerStatusReceiver.EXTRA_PACKAGE_NAME"
        const val EXTRA_VERSION_CODE =
            "me.timschneeberger.shizustore.data.receiver.InstallerStatusReceiver.EXTRA_VERSION_CODE"
    }
}
