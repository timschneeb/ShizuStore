/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import me.timschneeberger.shizustore.data.api.Availability

data class ResolvedApp(
    val packageName: String,
    val repoId: Int,
    val repoName: String,
    val repoAddress: String,
    val name: String,
    val summary: String,
    val authorName: String?,
    val iconUrl: String?,
    val versionCode: Long,
    val versionName: String,
    val signer: String?,
    val apkName: String,
    val hash: String,
    val size: Long,
    val minSdk: Int,
    val installedVersionCode: Long?,
    val installedSigner: String?,
    /**
     * The package the app is actually installed under. The entry is keyed by its
     * canonical package, but a flavor installs under its own, so installed-app
     * actions (open, uninstall, app info) must target this when non-null.
     */
    val installedPackage: String? = null,
    val channelRank: Int = CHANNEL_STABLE,
    val installedChannelRank: Int = CHANNEL_STABLE,
    val releasedAt: Long = 0L,
    val installedVersionName: String? = null,
    val slug: String = "",
    val iconHash: String? = null,
    val availability: Availability = Availability.DIRECT_APK,
    val storeUrl: String? = null,
    val url: String? = null,
    val sourceUrl: String? = null,
    val iconAdaptive: Boolean = false,
    val signerMd5: String? = null,
    val updateAvailable: Boolean = false,
    val updateCandidateId: Long? = null,
    val candidateId: Long? = null,
    val hasPaid: Boolean = false,
    val hasIap: Boolean = false,
    val stars: Int? = null,
    val installCount: Long = 0
) {
    val isInstalled: Boolean get() = installedVersionCode != null

    val isSameChannelAsInstalled: Boolean get() = channelRank == installedChannelRank

    /** The candidate shares the installed signing identity (set intersection, not equality). */
    private val signerMatchesInstalled: Boolean
        get() = installedSigner != null &&
            fingerprintSetIntersects(signer, parseFingerprintSet(installedSigner))

    val hasUpdate: Boolean
        get() = updateAvailable ||
            (
                installedVersionCode != null &&
                    versionCode > installedVersionCode &&
                    signerMatchesInstalled &&
                    isSameChannelAsInstalled
                )

    val signerDiffersFromInstalled: Boolean
        get() = installedSigner != null && !signerMatchesInstalled

    companion object {
        const val CHANNEL_STABLE = 0
        const val CHANNEL_BETA = 1
        const val CHANNEL_ALPHA = 2
    }
}
