/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import me.timschneeberger.shizustore.data.api.ApiResult
import me.timschneeberger.shizustore.data.api.InstallType
import me.timschneeberger.shizustore.data.api.ShizuApi
import me.timschneeberger.shizustore.data.repository.AppRepository
import me.timschneeberger.shizustore.util.Preferences

/**
 * Reports successful installs; once-guarded per (package, versionCode) so the
 * installer and reconciler paths cannot double-count. Honours
 * [Preferences.PREFERENCE_INSTALL_REPORTING], which is on by default.
 */
@Singleton
class InstallReporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val api: ShizuApi
) {
    private val reported: MutableSet<String> = ConcurrentHashMap.newKeySet()

    suspend fun reportInstalled(packageName: String, versionCode: Long, appSlug: String? = null) {
        val enabled = runCatching {
            Preferences.readBoolean(context, Preferences.PREFERENCE_INSTALL_REPORTING, true)
        }.getOrDefault(true)
        if (!enabled) {
            Log.i(TAG, "Not reporting install of $packageName: reporting is disabled")
            return
        }

        val key = "$packageName@$versionCode"
        if (!reported.add(key)) return

        // The download row names the listing the user installed from. Legacy rows
        // fall back to signature-aware package resolution.
        val slug = runCatching {
            appSlug?.takeIf { it.isNotBlank() }?.let { appRepository.get(it) }?.slug
                ?: appRepository.resolveByPackage(packageName)?.slug
        }.getOrNull()
        if (slug.isNullOrBlank()) {
            Log.i(TAG, "Not reporting install of $packageName: no catalog entry")
            return
        }

        val installType = runCatching {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            resolveInstallType(info.firstInstallTime, info.lastUpdateTime)
        }.getOrDefault(InstallType.UNKNOWN)

        when (
            val result = runCatching {
                api.reportInstall(slug, versionCode, installType)
            }.getOrNull()
        ) {
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

/**
 * PackageManager stamps [lastUpdateTime] on updates, so identical install times
 * mean a fresh install. Zeroed times (missing record or test double) read as
 * unknown rather than pretending to be fresh.
 */
internal fun resolveInstallType(firstInstallTime: Long, lastUpdateTime: Long): InstallType = when {
    firstInstallTime <= 0L || lastUpdateTime <= 0L -> InstallType.UNKNOWN
    firstInstallTime != lastUpdateTime -> InstallType.UPDATE
    else -> InstallType.FRESH
}
