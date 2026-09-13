/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.permission

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.util.Preferences

@Composable
fun rememberNotificationPermissionRequest(): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    return remember(context, scope) {
        {
            scope.launch {
                if (shouldRequestNotificationPermission(context)) {
                    Preferences.putBoolean(context, Preferences.PREFERENCE_NOTIFICATION_ASKED, true)
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            Unit
        }
    }
}

internal suspend fun shouldRequestNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !Preferences.readBoolean(context, Preferences.PREFERENCE_NOTIFICATION_ASKED)
