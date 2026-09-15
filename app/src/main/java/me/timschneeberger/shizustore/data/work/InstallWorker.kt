/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.work

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.pm.PackageInfoCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeoutOrNull
import me.timschneeberger.shizustore.data.helper.InstallDispatcher
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.model.InstallDispatch
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.util.NotificationUtil
import me.timschneeberger.shizustore.util.SHIZU_STORE_PACKAGE

@HiltWorker
class InstallWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val installDispatcher: InstallDispatcher,
    private val downloadDao: DownloadDao
) : CoroutineWorker(context, params) {
    @VisibleForTesting
    internal var settleTimeoutMs: Long = SETTLE_TIMEOUT_MS

    override suspend fun doWork(): Result {
        val packageName = inputData.getString(KEY_PACKAGE_NAME)
        if (packageName == null) {
            Log.e(TAG, "No package name in the input data; nothing to install")
            return Result.failure()
        }

        return try {
            dispatchAndWait(packageName)
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            Log.e(TAG, "Unexpected failure installing $packageName; the batch continues", throwable)
            Result.success()
        }
    }

    private suspend fun dispatchAndWait(packageName: String): Result {
        val downloadError = inputData.getString(DownloadWorker.KEY_ERROR)
        if (downloadError != null) {
            Log.i(TAG, "Not installing $packageName: its download failed with $downloadError")
            return Result.success()
        }

        val before = downloadDao.getDownload(packageName)

        if (
            isSelfUpdateSatisfied(
                packageName,
                before?.versionCode,
                installedVersionCode(packageName)
            )
        ) {
            Log.i(TAG, "$packageName already runs ${before?.versionCode}; skipping self-update")
            return Result.success()
        }

        when (val dispatch = installDispatcher.dispatch(packageName)) {
            InstallDispatch.Started -> Unit

            is InstallDispatch.Refused -> {
                onRefused(packageName, before?.displayName, dispatch)
                return Result.success()
            }
        }

        val settled = awaitSettled(packageName, before?.status)
        if (settled == null) {
            Log.w(
                TAG,
                "$packageName had not settled after ${settleTimeoutMs}ms; releasing the link " +
                    "and leaving the row to InstallReconciler"
            )
        } else {
            Log.i(TAG, "$packageName settled at $settled")
        }

        return Result.success()
    }

    private fun onRefused(
        packageName: String,
        displayName: String?,
        refusal: InstallDispatch.Refused
    ) {
        Log.w(TAG, "Install of $packageName refused at ${refusal.status}")
        if (refusal.status in SILENT_REFUSALS) return

        NotificationUtil.notifyApp(
            applicationContext,
            packageName,
            NotificationUtil.installRefusedNotification(
                applicationContext,
                displayName ?: packageName
            )
        )
    }

    private suspend fun awaitSettled(
        packageName: String,
        before: DownloadStatus?
    ): DownloadStatus? = withTimeoutOrNull(settleTimeoutMs) {
        var sawInstalling = false

        downloadDao.downloads()
            .mapNotNull { rows -> rows.firstOrNull { it.packageName == packageName }?.status }
            .distinctUntilChanged()
            .first { status ->
                if (status in DownloadStatus.installing) {
                    sawInstalling = true
                    false
                } else {
                    sawInstalling || status != before
                }
            }
    }

    private fun installedVersionCode(packageName: String): Long? = runCatching {
        PackageInfoCompat.getLongVersionCode(
            applicationContext.packageManager.getPackageInfo(packageName, 0)
        )
    }.getOrNull()

    companion object {
        const val KEY_PACKAGE_NAME = "packageName"

        private const val TAG = "InstallWorker"

        private val SILENT_REFUSALS: Set<DownloadStatus> = setOf(
            DownloadStatus.QUEUED,
            DownloadStatus.DOWNLOADING,
            DownloadStatus.VERIFYING,
            DownloadStatus.INSTALLING,
            DownloadStatus.AWAITING_CONFIRMATION,
            DownloadStatus.CANCELLED
        )

        @VisibleForTesting
        internal const val SETTLE_TIMEOUT_MS = 5L * 60L * 1_000L

        fun request(packageName: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<InstallWorker>()
                .setInputData(workDataOf(KEY_PACKAGE_NAME to packageName))
                .addTag(WorkTags.forPackage(packageName))
                .build()
    }
}

/**
 * A self-update that already landed must not be dispatched again: WorkManager can reschedule
 * this worker after the replacement killed the process.
 */
internal fun isSelfUpdateSatisfied(
    packageName: String,
    rowVersionCode: Long?,
    installedVersionCode: Long?
): Boolean = packageName == SHIZU_STORE_PACKAGE &&
    rowVersionCode != null &&
    rowVersionCode != 0L &&
    installedVersionCode == rowVersionCode
