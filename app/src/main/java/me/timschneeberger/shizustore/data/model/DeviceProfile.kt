/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import android.os.Build

object DeviceProfile {

    val abis: List<String> = Build.SUPPORTED_ABIS.toList()

    val sdkInt: Int = Build.VERSION.SDK_INT

    fun supports(nativeCode: List<String>): Boolean =
        nativeCode.isEmpty() || nativeCode.any { it in abis }

    fun runs(minSdk: Int, maxSdk: Int?): Boolean =
        minSdk <= sdkInt && (maxSdk == null || maxSdk == 0 || maxSdk >= sdkInt)

    fun abiLabel(nativeCode: List<String>): String? = when {
        nativeCode.isEmpty() -> null
        else -> nativeCode.sortedBy { abis.indexOf(it).takeIf { i -> i >= 0 } ?: abis.size }
            .joinToString(", ")
    }
}
