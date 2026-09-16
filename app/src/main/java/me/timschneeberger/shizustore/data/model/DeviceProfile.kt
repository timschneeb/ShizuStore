/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import android.os.Build

object DeviceProfile {

    val abis: List<String> = Build.SUPPORTED_ABIS.toList()

    val sdkInt: Int = Build.VERSION.SDK_INT

    fun supports(nativeCode: List<String>): Boolean = nativeCode.isEmpty() ||
        nativeCode.any { code -> abis.any { it.equals(code, ignoreCase = true) } }

    fun isIncompatible(minSdk: Int, deviceSdk: Int = sdkInt): Boolean =
        minSdk > 0 && minSdk > deviceSdk
}
