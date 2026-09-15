/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

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
    val isPrimary: Boolean
) {
    val isArchive: Boolean get() = !archiveEntry.isNullOrBlank()

    /** Only a candidate whose signing set contains the installed fingerprint is installable. */
    fun matchesInstalled(installed: CertFingerprint?): Boolean {
        if (installed == null) return false
        return fingerprintMatches(installed, sigSha256, sigMd5)
    }

    fun isNewerThan(installedVersionCode: Long?): Boolean =
        installedVersionCode != null && versionCode != null && versionCode > installedVersionCode

    companion object {

        fun from(entity: AppDownloadEntity, packageName: String?): AppCandidate = AppCandidate(
            id = entity.id,
            appSlug = entity.appSlug,
            packageName = packageName,
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
            isPrimary = entity.isPrimary
        )
    }
}
