/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import me.timschneeberger.shizustore.data.api.Availability

data class ResolvedApp(
    val packageName: String,
    val repoName: String,
    val name: String,
    val summary: String,
    val iconUrl: String?,
    val versionCode: Long,
    val versionName: String,
    val signer: String?,
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
    val slug: String = "",
    val availability: Availability = Availability.DIRECT_APK,
    val iconAdaptive: Boolean = false,
    val updateAvailable: Boolean = false,
    val candidateId: Long? = null,
    val hasPaid: Boolean = false,
    val hasIap: Boolean = false,
    val stars: Int? = null,
    val installCount: Long = 0
) {
    // Parsed once: hasUpdate and the signer checks run per row and re-parsing the
    // space-joined sets on every read shows up in profiles.
    private val installedFingerprint: Set<String> = parseFingerprintSet(installedSigner)
    private val candidateFingerprint: Set<String> = parseFingerprintSet(signer)

    val isInstalled: Boolean get() = installedVersionCode != null

    /** The candidate shares the installed signing identity (set intersection, not equality). */
    private val signerMatchesInstalled: Boolean
        get() = installedSigner != null && candidateFingerprint.any { it in installedFingerprint }

    val hasUpdate: Boolean
        get() = updateAvailable ||
            (
                installedVersionCode != null &&
                    versionCode > installedVersionCode &&
                    signerMatchesInstalled
                )

    val signerDiffersFromInstalled: Boolean
        get() = installedSigner != null && !signerMatchesInstalled
}
