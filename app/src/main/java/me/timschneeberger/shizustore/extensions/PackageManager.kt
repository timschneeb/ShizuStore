/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.extensions

import android.content.pm.PackageManager
import me.timschneeberger.shizustore.BuildConfig

fun PackageManager.getUpdateOwnerPackageNameCompat(packageName: String): String? {
    if (packageName == BuildConfig.APPLICATION_ID) return BuildConfig.APPLICATION_ID

    return when {
        isUAndAbove -> {
            val installSourceInfo = getInstallSourceInfo(packageName)
            installSourceInfo.updateOwnerPackageName ?: installSourceInfo.installingPackageName
        }

        isRAndAbove -> {
            val installSourceInfo = getInstallSourceInfo(packageName)
            installSourceInfo.installingPackageName
        }

        else -> {
            @Suppress("DEPRECATION")
            getInstallerPackageName(packageName)
        }
    }
}
