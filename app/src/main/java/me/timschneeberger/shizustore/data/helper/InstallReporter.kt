/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.api.ApiResult
import me.timschneeberger.shizustore.data.api.ShizuApi
import me.timschneeberger.shizustore.data.room.dao.AppDao

/**
 * Reports successful installs; once-guarded per (package, versionCode) so the
 * installer and reconciler paths cannot double-count.
 */
@Singleton
class InstallReporter @Inject constructor(
    private val appDao: AppDao,
    private val api: ShizuApi
) {
    private val reported: MutableSet<String> = ConcurrentHashMap.newKeySet()

    suspend fun reportInstalled(packageName: String, versionCode: Long) {
        val key = "$packageName@$versionCode"
        if (!reported.add(key)) return

        val slug = runCatching { appDao.getByPackage(packageName)?.slug }.getOrNull()
        if (slug.isNullOrBlank()) {
            Log.i(TAG, "Not reporting install of $packageName: no catalog entry")
            return
        }

        when (val result = runCatching { api.reportInstall(slug) }.getOrNull()) {
            is ApiResult.Success -> Log.i(
                TAG,
                "Reported install of $slug; server total is now ${result.value.installCount}"
            )
            else -> Log.w(TAG, "Failed to report install of $slug: $result")
        }
    }

    private companion object {
        const val TAG = "InstallReporter"
    }
}
