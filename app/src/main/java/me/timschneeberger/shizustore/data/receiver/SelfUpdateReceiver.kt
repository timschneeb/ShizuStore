/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.data.helper.InstallReconciler
import me.timschneeberger.shizustore.data.repository.InstalledRepository
import me.timschneeberger.shizustore.util.isolate
import me.timschneeberger.shizustore.util.isolatedIoScope

/**
 * A self-update replaces this process, so the dynamic [PackageManagerReceiver] cannot settle
 * the row. This manifest receiver catches ACTION_MY_PACKAGE_REPLACED on the next start.
 */
@AndroidEntryPoint
class SelfUpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var installedRepository: InstalledRepository

    @Inject
    lateinit var installReconciler: InstallReconciler

    private val scope = isolatedIoScope(TAG)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val packageName = context.packageName
        val pendingResult = goAsync()

        scope.launch {
            try {
                isolate(TAG, "settle $packageName") {
                    installReconciler.onPackageInstalled(packageName)
                }
                isolate(TAG, "mirror $packageName") {
                    installedRepository.onPackageAdded(packageName)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "SelfUpdateReceiver"
    }
}
