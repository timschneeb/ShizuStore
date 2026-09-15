/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.permission

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.BuildConfig
import me.timschneeberger.shizustore.extensions.isIgnoringBatteryOptimizations
import me.timschneeberger.shizustore.util.Preferences

private const val TAG = "DozeExemption"

/**
 * Asks to be left out of battery optimisation, once.
 *
 * A foreground service is enough on stock devices, but several vendors freeze a
 * backgrounded app and stop the transfer mid-way, so the ask belongs at the start
 * of a download.
 */
@Composable
fun rememberDozeExemptionRequest(): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    return remember(context, scope) {
        {
            scope.launch {
                if (shouldRequestDozeExemption(context)) {
                    Preferences.putBoolean(context, Preferences.PREFERENCE_DOZE_ASKED, true)
                    runCatching { launcher.launch(dozeSettingsIntent()) }
                        .onFailure { Log.w(TAG, "No screen to grant the exemption on", it) }
                }
            }
            Unit
        }
    }
}

internal suspend fun shouldRequestDozeExemption(context: Context): Boolean =
    !context.isIgnoringBatteryOptimizations() &&
        !Preferences.readBoolean(context, Preferences.PREFERENCE_DOZE_ASKED)

/** The system screen is the only way in: an app cannot grant itself the exemption. */
@SuppressLint("BatteryLife")
private fun dozeSettingsIntent(): Intent = Intent(
    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    "package:${BuildConfig.APPLICATION_ID}".toUri()
)
