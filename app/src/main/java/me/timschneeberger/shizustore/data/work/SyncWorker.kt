/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure
import me.timschneeberger.shizustore.data.sync.CatalogSyncOutcome
import me.timschneeberger.shizustore.data.sync.CatalogSyncer

/** Single-flight catalog sync into Room. Fails fast on connection loss, retries rate limits. */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncer: CatalogSyncer
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = when (val outcome = syncer.sync()) {
        is CatalogSyncOutcome.Success -> Result.success(
            workDataOf(
                KEY_ADDED to outcome.added,
                KEY_UPDATED to outcome.updated,
                KEY_REMOVED to outcome.removed
            )
        )

        CatalogSyncOutcome.AlreadyRunning -> Result.success()

        is CatalogSyncOutcome.Failed -> when (outcome.failure) {
            // Fail fast on connection loss: the attempt already surfaced the
            // error, and retry() would flap the spinner forever. The user
            // retries manually (pull-to-refresh / retry button). Rate limits
            // stay retried: they are transient and server-driven.
            CatalogSyncFailure.NETWORK -> Result.failure(workDataOf(KEY_ERROR to outcome.failure.name))
            CatalogSyncFailure.RATE_LIMITED -> Result.retry()
            else -> Result.failure(workDataOf(KEY_ERROR to outcome.failure.name))
        }
    }

    companion object {
        const val KEY_ADDED = "added"
        const val KEY_UPDATED = "updated"
        const val KEY_REMOVED = "removed"
        const val KEY_ERROR = "error"

        const val UNIQUE_NAME = "sync-catalog"
        const val TAG = "sync-catalog"

        fun enqueue(
            context: Context,
            requireUnmetered: Boolean = false,
            expedited: Boolean = false
        ) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .addTag(TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(
                            if (requireUnmetered) NetworkType.UNMETERED else NetworkType.CONNECTED
                        )
                        .build()
                )
                .apply {
                    if (expedited) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
