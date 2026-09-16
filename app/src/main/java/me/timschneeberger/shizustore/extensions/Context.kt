/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri

private const val TAG = "Context"

fun Context.viewExternal(url: String): Boolean = try {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    true
} catch (_: Exception) {
    Log.e(TAG, "No app to handle $url")
    false
}

fun Context.shareApp(displayName: String, packageName: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, displayName)
        putExtra(Intent.EXTRA_TEXT, "https://f-droid.org/packages/$packageName/")
    }
    runCatching { startActivity(Intent.createChooser(intent, displayName)) }
        .onFailure { Log.e(TAG, "No app to share $packageName with") }
}

fun Context.uninstallPackage(packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())
    runCatching { startActivity(intent) }
        .onFailure { Log.w(TAG, "Could not uninstall $packageName", it) }
}

fun Context.appInfo(packageName: String) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
        }
        startActivity(intent)
    } catch (exception: Exception) {
        Log.e(TAG, "Failed to open app info", exception)
    }
}

fun Context.isIgnoringBatteryOptimizations(): Boolean =
    getSystemService<PowerManager>()?.isIgnoringBatteryOptimizations(packageName) ?: true

fun Context.checkManifestPermission(permission: String): Boolean =
    ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
