/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import androidx.room.Embedded
import androidx.room.Ignore

enum class ReleaseChannel { STABLE, BETA, ALPHA }

fun releaseChannelOf(channels: List<String>): ReleaseChannel {
    val lowered = channels.map { it.lowercase() }
    return when {
        lowered.any { it.contains("alpha") } -> ReleaseChannel.ALPHA
        lowered.any { it.contains("beta") } -> ReleaseChannel.BETA
        else -> ReleaseChannel.STABLE
    }
}

data class AppSource(
    @Embedded val app: ResolvedApp,
    val added: Long,
    val releaseChannels: List<String>,
    val nativeCode: List<String> = emptyList(),
    val maxSdk: Int? = null,
    @Ignore val signerMatch: Boolean = false
) {
    val channel: ReleaseChannel get() = releaseChannelOf(releaseChannels)

    val isInstalled: Boolean get() = app.installedVersionCode == app.versionCode

    val abiLabel: String? get() = DeviceProfile.abiLabel(nativeCode)

    val runsOnThisDevice: Boolean
        get() = DeviceProfile.supports(nativeCode) && DeviceProfile.runs(app.minSdk, maxSdk)

    val isSameChannelAsInstalled: Boolean get() = app.isSameChannelAsInstalled
}

fun List<AppSource>.preferredForThisDevice(): AppSource? =
    firstOrNull { it.runsOnThisDevice && it.signerMatch }
        ?: firstOrNull { it.runsOnThisDevice && !it.app.signerDiffersFromInstalled }
        ?: firstOrNull { it.runsOnThisDevice }
        ?: firstOrNull()
