/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import androidx.compose.runtime.Immutable
import me.timschneeberger.shizustore.data.api.Availability

// Every field is set once at construction; the annotation lets list rows skip
// recomposition instead of treating the List fields as unknown state.
@Immutable
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
    /** True while the user ignores the offered update; badges hide, manual installs stay. */
    val updateIgnored: Boolean = false,
    val candidateId: Long? = null,
    val hasPaid: Boolean = false,
    val hasIap: Boolean = false,
    val hasAds: Boolean = false,
    val dhizukuDeclared: Boolean = false,
    val trackers: List<String> = emptyList(),
    /** Locale label that differs from [name], when the APK ships one for a device locale. */
    val localizedName: String? = null,
    val stars: Int? = null,
    val installCount: Long = 0,
    /** Installs in the server's trending window; null when the app is not ranked. */
    val trendScore: Long? = null,
    val downloadTotal: Long? = null,
    val categorySlug: String? = null,
    /** Parsed from the ISO-8601 wire strings; null when the source has no date. */
    val listUpdatedAtMillis: Long? = null,
    val versionUpdatedAtMillis: Long? = null
) {
    // Parsed once: hasManualUpdate and the signer checks run per row and re-parsing the
    // space-joined sets on every read shows up in profiles.
    private val installedFingerprint: Set<String> = parseFingerprintSet(installedSigner)
    private val candidateFingerprint: Set<String> = parseFingerprintSet(signer)

    val isInstalled: Boolean get() = installedVersionCode != null

    /** Localized name when the APK ships one, else the catalog name or package. */
    val displayName: String
        get() = localizedName?.takeIf { it.isNotBlank() } ?: name.ifBlank { packageName }

    /** The candidate shares the installed signing identity (set intersection, not equality). */
    private val signerMatchesInstalled: Boolean
        get() = installedSigner != null && candidateFingerprint.any { it in installedFingerprint }

    /** Manual installs stay possible from details even while the update is ignored. */
    val hasManualUpdate: Boolean
        get() = updateAvailable ||
            (
                installedVersionCode != null &&
                    versionCode > installedVersionCode &&
                    signerMatchesInstalled
                )

    /** Badge and nagging surfaces hide an ignored update; the Update action does not. */
    val hasUpdate: Boolean
        get() = hasManualUpdate && !updateIgnored

    val signerDiffersFromInstalled: Boolean
        get() = installedSigner != null && !signerMatchesInstalled
}
