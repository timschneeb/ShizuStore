/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
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

@AndroidEntryPoint
class PackageManagerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var installedRepository: InstalledRepository

    @Inject
    lateinit var installReconciler: InstallReconciler

    private val scope = isolatedIoScope(TAG)

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val pendingResult = goAsync()

        scope.launch {
            try {
                when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REPLACED -> {
                        isolate(TAG, "settle $packageName") {
                            installReconciler.onPackageInstalled(packageName)
                        }
                        isolate(TAG, "mirror $packageName") {
                            installedRepository.onPackageAdded(packageName)
                        }
                    }

                    Intent.ACTION_PACKAGE_REMOVED ->
                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                            isolate(TAG, "forget $packageName") {
                                installedRepository.onPackageRemoved(packageName)
                            }
                        }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "PackageManagerReceiver"
    }
}
