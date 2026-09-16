/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.extensions

import android.annotation.SuppressLint
import android.os.Build

val isOAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

val isPAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

val isQAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

val isRAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

val isSAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val isTAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

val isUAndAbove: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

val isOneUI: Boolean
    get() = !getSystemProperty("ro.build.version.oneui").isNullOrBlank() ||
        Build.MANUFACTURER.equals("samsung", ignoreCase = true)

@SuppressLint("PrivateApi")
private fun getSystemProperty(key: String): String? = try {
    Class.forName("android.os.SystemProperties")
        .getDeclaredMethod("get", String::class.java)
        .invoke(null, key) as String
} catch (_: Exception) {
    null
}
