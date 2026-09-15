/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's DownloadWorker (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.work

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_PARTIAL
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.timschneeberger.shizustore.data.download.ApkVerifier
import me.timschneeberger.shizustore.data.download.ArchiveExtractor
import me.timschneeberger.shizustore.data.model.DownloadError
import me.timschneeberger.shizustore.data.model.DownloadFailure
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.network.DataSize
import me.timschneeberger.shizustore.data.network.Downloader
import me.timschneeberger.shizustore.data.network.NetworkResponse
import me.timschneeberger.shizustore.data.network.percentBy
import me.timschneeberger.shizustore.data.room.dao.DownloadDao
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.extensions.isQAndAbove
import me.timschneeberger.shizustore.util.NotificationUtil
import me.timschneeberger.shizustore.util.PathUtil

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val downloader: Downloader
) : CoroutineWorker(context, params) {
    @Volatile
    private var current: Download? = null

    @Volatile
    private var cancelRequested = false

    private var urlsTried = 0

    private var lastEmittedAt = 0L
    private var lastEmittedBytes = 0L
    private var lastEmittedProgress = -1

    override suspend fun doWork(): Result {
        val packageName = inputData.getString(KEY_PACKAGE_NAME) ?: return Result.failure()

        if (!claim(packageName)) {
            Log.i(TAG, "$packageName is already being downloaded; standing down")
            return Result.success()
        }

        return try {
            transfer(packageName)
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            Log.e(TAG, "Unexpected failure for $packageName; the batch continues", throwable)
            Result.success()
        } finally {
            release(packageName)
        }
    }

    private suspend fun transfer(packageName: String): Result {
        val download = downloadDao.getDownload(packageName) ?: run {
            Log.w(TAG, "No download row for $packageName; nothing to fetch")
            return Result.success()
        }

        if (download.status == DownloadStatus.CANCELLED) {
            Log.i(TAG, "$packageName was cancelled before its download ran; skipping")
            return Result.success()
        }

        current = download

        val target = PathUtil.getApkFile(applicationContext, packageName, download.versionCode)
        val source = sourceFile(download, target)

        ensureStorageAvailable(download.size)?.let {
            return onFailure(packageName, download, it, target)
        }

        runCatching { setForeground(getForegroundInfo()) }
            .onFailure {
                Log.e(
                    TAG,
                    "$packageName is downloading without a foreground service, so the system may " +
                        "stop it while the app is in the background",
                    it
                )
            }

        downloadDao.updateStatusAndError(packageName, DownloadStatus.DOWNLOADING, null)

        return try {
            fetch(packageName, download, source, target)
        } catch (exception: CancellationException) {
            if (cancelRequested) onStopped(packageName, download, target) else throw exception
        } catch (throwable: Throwable) {
            Log.e(TAG, "Unexpected failure for $packageName", throwable)
            onFailure(packageName, download, DownloadError.Network(throwable), target)
        }
    }

    /** The apk itself, or the staging file when the candidate is a zip with an `archiveEntry`. */
    private fun sourceFile(download: Download, target: File): File =
        if (download.archiveEntry.isNullOrBlank()) {
            target
        } else {
            PathUtil.getArchiveFile(applicationContext, download.packageName, download.versionCode)
        }

    private suspend fun fetch(
        packageName: String,
        download: Download,
        source: File,
        target: File
    ): Result {
        val urls = listOf(download.apkUrl)
        var lastError: DownloadError? = null

        for (url in urls) {
            if (isCancelledByUser(packageName)) return onCancelled(packageName, download, target)
            urlsTried++

            var resumeFromDisk = false
            var attempt = 0

            while (attempt < MAX_ATTEMPTS_PER_URL) {
                attempt++
                resetProgressWindow()

                val response = downloader.downloadToFile(
                    url = url,
                    target = source,
                    resume = resumeFromDisk
                ) { read, total ->
                    onProgress(packageName, read, total)
                }

                val error: DownloadError? = when (response) {
                    is NetworkResponse.Success -> null
                    is NetworkResponse.Error.Http -> DownloadError.Http(response.statusCode)
                    is NetworkResponse.Error.ConnectionTimeout ->
                        DownloadError.Network(response.exception)

                    is NetworkResponse.Error.SocketTimeout ->
                        DownloadError.Network(response.exception)

                    is NetworkResponse.Error.IO -> DownloadError.Network(response.exception)
                    is NetworkResponse.Error.Unknown -> DownloadError.Network(response.exception)
                }

                if (response is NetworkResponse.Success) {
                    if (resumeFromDisk && response.statusCode != HTTP_PARTIAL) {
                        Log.w(TAG, "$url ignored Range (${response.statusCode}), restarting")
                        source.delete()
                        resumeFromDisk = false
                        attempt--
                        continue
                    }
                    return verifyAndFinish(packageName, download, source, target)
                }

                val failure = error ?: DownloadError.MirrorExhausted(urls.size)
                Log.w(TAG, "Attempt $attempt for $packageName at $url failed: $failure")
                lastError = failure

                if (!isRetryable(failure) || attempt >= MAX_ATTEMPTS_PER_URL) break

                resumeFromDisk = true
                delay(RETRY_DELAY_MS * attempt)
            }
        }

        return onFailure(
            packageName,
            download,
            lastError ?: DownloadError.MirrorExhausted(urls.size),
            target
        )
    }

    private suspend fun verifyAndFinish(
        packageName: String,
        download: Download,
        source: File,
        target: File
    ): Result {
        downloadDao.updateStatus(packageName, DownloadStatus.VERIFYING)

        val mismatch = ApkVerifier.verify(source, download.hash, download.hashType)
        if (mismatch != null) {
            cleanup(download, target)
            Log.e(TAG, "Hash mismatch for $packageName, deleted ${source.name}")
            return onFailure(packageName, download, mismatch, target)
        }

        val entry = download.archiveEntry
        if (!entry.isNullOrBlank()) {
            val extracted = ArchiveExtractor.extract(source, entry, target)
            if (!extracted) {
                cleanup(download, target)
                Log.e(TAG, "Could not extract $entry for $packageName")
                return onFailure(packageName, download, DownloadError.Archive(entry), target)
            }
            source.delete()
        }

        return onSuccess(packageName)
    }

    private suspend fun onSuccess(packageName: String): Result = withContext(NonCancellable) {
        downloadDao.updateProgress(packageName, 100, 0L, 0L)
        downloadDao.updateStatus(packageName, DownloadStatus.COMPLETED)
        cancelNotification(packageName)
        postTerminalNotification { NotificationUtil.completedNotification(applicationContext, it) }
        Result.success()
    }

    private suspend fun onFailure(
        packageName: String,
        download: Download,
        error: DownloadError,
        target: File
    ): Result = withContext(NonCancellable) {
        if (isRetryable(error) && runAttemptCount < MAX_DOWNLOAD_RETRIES) {
            Log.w(TAG, "Transient failure for $packageName ($error), retrying")
            return@withContext Result.retry()
        }

        cleanup(download, target)

        Log.e(TAG, "Download failed for $packageName: $error")
        downloadDao.updateStatusAndError(
            packageName,
            DownloadStatus.FAILED,
            DownloadFailure.of(error)
        )
        cancelNotification(packageName)
        postTerminalNotification { NotificationUtil.failedNotification(applicationContext, it) }

        Result.success(failureData(error))
    }

    private suspend fun onStopped(packageName: String, download: Download, target: File): Result =
        withContext(NonCancellable) {
            if (downloadDao.getDownload(packageName)?.status == DownloadStatus.CANCELLED) {
                onCancelled(packageName, download, target)
            } else {
                Log.i(TAG, "Stopped by the system for $packageName, leaving the row alone")
                Result.retry()
            }
        }

    private suspend fun onCancelled(packageName: String, download: Download, target: File): Result =
        withContext(NonCancellable) {
            cleanup(download, target)
            downloadDao.updateStatusAndError(packageName, DownloadStatus.CANCELLED, null)
            downloadDao.updateProgress(packageName, 0, 0L, 0L)
            cancelNotification(packageName)

            Result.success()
        }

    /** Removes both the apk target and any staged archive so a failed attempt leaves nothing behind. */
    private fun cleanup(download: Download, target: File) {
        target.delete()
        if (!download.archiveEntry.isNullOrBlank()) {
            PathUtil.getArchiveFile(applicationContext, download.packageName, download.versionCode)
                .delete()
        }
    }

    private fun failureData(error: DownloadError): Data = Data.Builder()
        .putString(KEY_ERROR, error::class.simpleName)
        .putInt(KEY_URLS_TRIED, urlsTried)
        .apply {
            when (error) {
                is DownloadError.Http -> putInt(KEY_HTTP_CODE, error.code)

                is DownloadError.Network -> putString(
                    KEY_CAUSE,
                    "${error.cause::class.simpleName}: ${error.cause.message}"
                        .take(MAX_CAUSE_CHARS)
                )

                is DownloadError.HashMismatch -> {
                    putString(KEY_EXPECTED_HASH, error.expected.take(HASH_PREFIX_CHARS))
                    putString(KEY_ACTUAL_HASH, error.actual.take(HASH_PREFIX_CHARS))
                }

                is DownloadError.Archive -> putString(KEY_CAUSE, error.entry.take(MAX_CAUSE_CHARS))

                is DownloadError.InsufficientStorage -> {
                    putLong(KEY_REQUIRED_BYTES, error.requiredBytes)
                    putLong(KEY_AVAILABLE_BYTES, error.availableBytes)
                }

                is DownloadError.MirrorExhausted -> Unit
            }
        }
        .build()

    private suspend fun isCancelledByUser(packageName: String): Boolean = runCatching {
        downloadDao.getDownload(packageName)?.status == DownloadStatus.CANCELLED
    }.getOrDefault(false)

    private fun isRetryable(error: DownloadError?): Boolean = when (error) {
        null -> false
        is DownloadError.Network -> isTransient(error.cause)
        is DownloadError.Http ->
            error.code == HTTP_CLIENT_TIMEOUT ||
                error.code == HTTP_TOO_MANY_REQUESTS ||
                error.code >= HTTP_INTERNAL_ERROR

        is DownloadError.HashMismatch -> false
        is DownloadError.Archive -> false
        is DownloadError.InsufficientStorage -> false
        is DownloadError.MirrorExhausted -> false
    }

    private fun isTransient(throwable: Throwable?): Boolean = when (throwable) {
        null -> false
        is IOException -> true
        else -> isTransient(throwable.cause)
    }

    private fun resetProgressWindow() {
        lastEmittedAt = 0L
        lastEmittedBytes = 0L
        lastEmittedProgress = -1
    }

    private suspend fun onProgress(packageName: String, read: DataSize, total: DataSize?) {
        if (isStopped) {
            cancelRequested = true
            throw CancellationException("Download worker was stopped")
        }

        val now = System.currentTimeMillis()
        val elapsed = now - lastEmittedAt
        val progress = read percentBy total

        if (lastEmittedAt != 0L && elapsed < PROGRESS_INTERVAL_MS && progress < 100) return

        val speed = if (lastEmittedAt == 0L || elapsed <= 0L) {
            0L
        } else {
            (read.value - lastEmittedBytes) * MILLIS_PER_SECOND / elapsed
        }
        val remaining = (total?.value ?: 0L) - read.value
        val timeRemaining = if (speed > 0L && remaining > 0L) {
            remaining * MILLIS_PER_SECOND / speed
        } else {
            0L
        }

        lastEmittedAt = now
        lastEmittedBytes = read.value

        if (progress >= 0 && progress != lastEmittedProgress) {
            lastEmittedProgress = progress
            downloadDao.updateProgress(packageName, progress, speed, timeRemaining)
            setProgressAsync(workDataOf(KEY_PROGRESS to progress))

            current = current?.copy(
                status = DownloadStatus.DOWNLOADING,
                progress = progress,
                speed = speed,
                timeRemaining = timeRemaining
            )?.also { updateNotification(it) }
        }

        if (isCancelledByUser(packageName)) {
            cancelRequested = true
            throw CancellationException("Download cancelled by the user")
        }
    }

    private fun ensureStorageAvailable(requiredBytes: Long): DownloadError.InsufficientStorage? {
        if (requiredBytes <= 0L) return null

        val available = PathUtil.getApkDir(applicationContext).usableSpace
        return if (available < requiredBytes) {
            DownloadError.InsufficientStorage(requiredBytes, available)
        } else {
            null
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationUtil.downloadNotification(
            applicationContext,
            current,
            grouped = isGrouped()
        )
        // Per-download id: workers sharing one id overwrote the same row,
        // so parallel downloads jumped between progresses.
        val id = NotificationUtil.notificationId(
            inputData.getString(KEY_PACKAGE_NAME).orEmpty()
        )
        return if (isQAndAbove) {
            ForegroundInfo(id, notification, FOREGROUND_SERVICE_TYPE)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    private suspend fun updateNotification(download: Download) {
        NotificationUtil.notifyApp(
            applicationContext,
            download.packageName,
            NotificationUtil.downloadNotification(
                applicationContext,
                download,
                grouped = isGrouped()
            )
        )
    }

    /** Groups only while two or more downloads are visibly progressing. */
    private suspend fun isGrouped(): Boolean = downloadDao.countByStatus(
        listOf(DownloadStatus.DOWNLOADING, DownloadStatus.VERIFYING)
    ) > 1

    private fun cancelNotification(packageName: String) {
        NotificationUtil.clearAppNotification(applicationContext, packageName)
    }

    private fun postTerminalNotification(build: (Download) -> Notification) {
        val download = current ?: return
        NotificationUtil.notifyApp(applicationContext, download.packageName, build(download))
    }

    companion object {
        const val KEY_PACKAGE_NAME = "packageName"
        const val KEY_PROGRESS = "progress"

        const val KEY_ERROR = "error"
        const val KEY_URLS_TRIED = "urlsTried"
        const val KEY_HTTP_CODE = "httpCode"
        const val KEY_CAUSE = "cause"
        const val KEY_EXPECTED_HASH = "expectedHash"
        const val KEY_ACTUAL_HASH = "actualHash"
        const val KEY_REQUIRED_BYTES = "requiredBytes"
        const val KEY_AVAILABLE_BYTES = "availableBytes"

        private const val TAG = "DownloadWorker"
        private const val HASH_PREFIX_CHARS = 16
        private const val MAX_CAUSE_CHARS = 200

        @SuppressLint("InlinedApi")
        const val FOREGROUND_SERVICE_TYPE = FOREGROUND_SERVICE_TYPE_DATA_SYNC

        private const val MAX_DOWNLOAD_RETRIES = 5

        private const val MAX_ATTEMPTS_PER_URL = 3

        private const val RETRY_DELAY_MS = 1_000L
        private const val PROGRESS_INTERVAL_MS = 500L
        private const val MILLIS_PER_SECOND = 1_000L

        private const val HTTP_TOO_MANY_REQUESTS = 429

        private val claimed = mutableSetOf<String>()

        private fun claim(packageName: String): Boolean =
            synchronized(claimed) { claimed.add(packageName) }

        private fun release(packageName: String) {
            synchronized(claimed) { claimed.remove(packageName) }
        }

        @VisibleForTesting
        internal fun clearClaims() {
            synchronized(claimed) { claimed.clear() }
        }

        fun uniqueName(packageName: String) = "download-$packageName"

        fun request(
            packageName: String,
            networkType: NetworkType = NetworkType.CONNECTED
        ): OneTimeWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf(KEY_PACKAGE_NAME to packageName))
            .addTag(WorkTags.forPackage(packageName))
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(networkType)
                    .build()
            )
            .build()

        fun enqueue(context: Context, packageName: String) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueName(packageName),
                ExistingWorkPolicy.KEEP,
                request(packageName)
            )
        }
    }
}
