/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import android.os.Build
import me.timschneeberger.shizustore.data.api.SourceKind
import me.timschneeberger.shizustore.data.room.entity.AppDownloadEntity

data class AppCandidate(
    val id: Long,
    val appSlug: String,
    val packageName: String?,
    val versionCode: Long?,
    val versionName: String?,
    val source: SourceKind?,
    val apkUrl: String,
    val archiveEntry: String?,
    val size: Long?,
    val sha256: String?,
    val sigSha256: String?,
    val sigMd5: String?,
    val minSdk: Int?,
    val abi: String? = null,
    val isPrimary: Boolean
) {
    /** Only a candidate whose signing set contains the installed fingerprint is installable. */
    fun matchesInstalled(installed: CertFingerprint?): Boolean {
        if (installed == null) return false
        return fingerprintMatches(installed, sigSha256, sigMd5)
    }

    /**
     * F-Droid index rows may pack several ABIs into one comma or space separated
     * string (a universal APK lists every ABI it contains).
     */
    val abiTokens: List<String>
        get() = abi?.split(',', ' ')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            .orEmpty()

    /** A universal candidate (no ABI) runs anywhere; an empty device list means "do not filter". */
    fun supportsAbi(supported: List<String>): Boolean = abiTokens.isEmpty() ||
        supported.isEmpty() ||
        abiTokens.any { token -> supported.any { it.equals(token, ignoreCase = true) } }

    fun isNewerThan(installedVersionCode: Long?): Boolean =
        installedVersionCode != null && versionCode != null && versionCode > installedVersionCode

    companion object {

        /** Device ABIs, safely empty off-device (JVM unit tests) so ABI never filters there. */
        val deviceAbis: List<String> by lazy {
            runCatching { Build.SUPPORTED_ABIS.toList() }.getOrElse { emptyList() }
        }

        /** `fallbackPackage` is the app's canonical package for legacy rows without one. */
        fun from(entity: AppDownloadEntity, fallbackPackage: String?): AppCandidate = AppCandidate(
            id = entity.id,
            appSlug = entity.appSlug,
            packageName = entity.packageName ?: fallbackPackage,
            versionCode = entity.versionCode,
            versionName = entity.versionName,
            source = entity.source,
            apkUrl = entity.apkUrl,
            archiveEntry = entity.archiveEntry,
            size = entity.size,
            sha256 = entity.sha256,
            sigSha256 = entity.sigSha256,
            sigMd5 = entity.sigMd5,
            minSdk = entity.minSdk,
            abi = entity.abi,
            isPrimary = entity.isPrimary
        )
    }
}
