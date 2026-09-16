/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppSource(
    val app: ResolvedApp,
    val nativeCode: List<String> = emptyList(),
    val signerMatch: Boolean = false,
    val installedPackageMatch: Boolean = false
) {
    val isInstalled: Boolean get() = installedPackageMatch &&
        app.installedVersionCode == app.versionCode

    val runsOnThisDevice: Boolean
        get() = DeviceProfile.supports(nativeCode) && !DeviceProfile.isIncompatible(app.minSdk)
}

fun List<AppSource>.preferredForThisDevice(): AppSource? =
    firstOrNull { it.installedPackageMatch && it.runsOnThisDevice }
        ?: firstOrNull { it.installedPackageMatch }
        ?: firstOrNull { it.runsOnThisDevice && it.signerMatch }
        ?: firstOrNull { it.runsOnThisDevice && !it.app.signerDiffersFromInstalled }
        ?: firstOrNull { it.runsOnThisDevice }
        ?: firstOrNull()
