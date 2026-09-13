/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's ShortcutManagerUtil (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.util

import android.content.Context
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap

object ShortcutUtil {

    fun requestPinShortcut(context: Context, packageName: String): Boolean {
        val packageManager = context.packageManager
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return false

        return runCatching {
            val info = packageManager.getApplicationInfo(packageName, 0)
            val shortcut = ShortcutInfoCompat.Builder(context, packageName)
                .setShortLabel(info.loadLabel(packageManager))
                .setIcon(IconCompat.createWithBitmap(info.loadIcon(packageManager).toBitmap()))
                .setIntent(launchIntent)
                .build()

            ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        }.onFailure {
            Log.w(TAG, "Could not request a home screen shortcut for $packageName", it)
        }.getOrDefault(false)
    }

    private const val TAG = "ShortcutUtil"
}
