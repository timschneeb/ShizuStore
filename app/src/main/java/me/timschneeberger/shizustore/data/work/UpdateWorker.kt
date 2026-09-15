/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import me.timschneeberger.shizustore.data.helper.UpdateBatch
import me.timschneeberger.shizustore.data.installer.AppInstaller
import me.timschneeberger.shizustore.data.installer.canInstallUnattended
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.repository.DetailedAppRepository
import me.timschneeberger.shizustore.data.repository.DetailedAppResult
import me.timschneeberger.shizustore.data.repository.UpdateStateRepository
import me.timschneeberger.shizustore.data.room.dao.AppDao
import me.timschneeberger.shizustore.data.room.dao.AppDownloadDao
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure
import me.timschneeberger.shizustore.data.sync.CatalogSyncOutcome
import me.timschneeberger.shizustore.data.sync.CatalogSyncer
import me.timschneeberger.shizustore.util.NotificationUtil
import me.timschneeberger.shizustore.util.Preferences

/** Periodic catalog refresh, unattended updates when possible, otherwise the update notification. */
@HiltWorker
class UpdateWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val syncer: CatalogSyncer,
    private val appDao: AppDao,
    private val appDownloadDao: AppDownloadDao,
    private val detailedAppRepository: DetailedAppRepository,
    private val updateStateRepository: UpdateStateRepository,
    private val updateBatch: UpdateBatch
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (val outcome = syncer.sync()) {
        is CatalogSyncOutcome.Failed -> when (outcome.failure) {
            // Same fail-fast policy as SyncWorker: connection loss settles as
            // a finished failure so the error UI sticks; only rate limits retry.
            CatalogSyncFailure.NETWORK -> Result.failure()
            CatalogSyncFailure.RATE_LIMITED -> Result.retry()
            else -> Result.failure()
        }

        else -> handleUpdatable()
    }

    private suspend fun handleUpdatable(): Result {
        val updatable = appDao.getAll().filter { it.updateAvailable && it.packageName != null }
        if (updatable.isEmpty()) {
            Log.i(TAG, "Nothing to update; posting nothing")
            return Result.success()
        }

        if (mayInstallUnattended() && enqueueUnattended(updatable) > 0) {
            return Result.success()
        }

        NotificationUtil.notify(
            context,
            NotificationUtil.UPDATES_NOTIFICATION_ID,
            NotificationUtil.updatesAvailableNotification(context, updatable.map { it.name })
        )
        return Result.success()
    }

    private suspend fun mayInstallUnattended(): Boolean =
        Preferences.readBoolean(context, Preferences.PREFERENCE_UPDATES_UNATTENDED) &&
            canInstallUnattended(AppInstaller.getCurrentInstaller(context))

    private suspend fun enqueueUnattended(updatable: List<AppEntity>): Int {
        var enqueued = 0
        for (app in updatable) {
            val candidate = resolveCandidate(app) ?: continue
            if (updateBatch.enqueue(app, candidate, unattended = true)) enqueued++
        }
        if (enqueued > 0) Log.i(TAG, "Queued $enqueued unattended update(s)")
        return enqueued
    }

    /** The worker only knows the installed version, so fetch details to pin the matching candidate. */
    private suspend fun resolveCandidate(app: AppEntity): AppCandidate? {
        val packageName = app.packageName ?: return null
        var current = app

        if (current.updateCandidateId == null) {
            if (detailedAppRepository.fetchAndPersist(current.slug) !is DetailedAppResult.Success) {
                return null
            }
            updateStateRepository.recompute(packageName)
            current = appDao.get(current.slug) ?: return null
        }

        val candidateId = current.updateCandidateId ?: return null
        val entity = appDownloadDao.getById(candidateId) ?: return null
        return AppCandidate.from(entity, packageName)
    }

    companion object {
        private const val TAG = "UpdateWorker"

        const val UNIQUE_NAME = "periodic_update_check"

        const val DEFAULT_INTERVAL_HOURS = 6

        const val INTERVAL_NEVER = 0

        suspend fun intervalHours(context: Context): Int = Preferences.readInteger(
            context,
            Preferences.PREFERENCE_UPDATES_CHECK_INTERVAL,
            DEFAULT_INTERVAL_HOURS
        )

        suspend fun schedule(context: Context) =
            armOrCancel(context, ExistingPeriodicWorkPolicy.KEEP)

        suspend fun reschedule(context: Context) =
            armOrCancel(context, ExistingPeriodicWorkPolicy.UPDATE)

        private suspend fun armOrCancel(context: Context, policy: ExistingPeriodicWorkPolicy) {
            val workManager = WorkManager.getInstance(context)
            val hours = intervalHours(context)

            if (hours == INTERVAL_NEVER) {
                workManager.cancelUniqueWork(UNIQUE_NAME)
                return
            }

            val request = PeriodicWorkRequestBuilder<UpdateWorker>(
                hours.toLong(),
                TimeUnit.HOURS
            ).setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(networkType(context))
                    .build()
            ).build()

            workManager.enqueueUniquePeriodicWork(UNIQUE_NAME, policy, request)
        }

        private suspend fun networkType(context: Context): NetworkType =
            if (Preferences.readBoolean(context, Preferences.PREFERENCE_SYNC_ON_WIFI_ONLY)) {
                NetworkType.UNMETERED
            } else {
                NetworkType.CONNECTED
            }
    }
}
