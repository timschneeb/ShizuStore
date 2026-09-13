/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.helper.DownloadHelper
import me.timschneeberger.shizustore.util.isolatedIoScope

@AndroidEntryPoint
class DownloadRetryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var downloadHelper: DownloadHelper

    private val scope = isolatedIoScope(TAG)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RETRY) return
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return

        Log.i(TAG, "Retrying $packageName from its notification")

        val pendingResult = goAsync()
        scope.launch {
            try {
                downloadHelper.retry(packageName)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "DownloadRetryReceiver"

        const val ACTION_RETRY =
            "me.timschneeberger.shizustore.data.receiver.DownloadRetryReceiver.RETRY"

        const val EXTRA_PACKAGE_NAME = DownloadCancelReceiver.EXTRA_PACKAGE_NAME
    }
}
