/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import me.timschneeberger.shizustore.data.model.AppCandidate
import me.timschneeberger.shizustore.data.model.DownloadFailure
import me.timschneeberger.shizustore.data.model.DownloadStatus

@Entity(tableName = "download")
data class Download(
    @PrimaryKey val packageName: String,
    val repoId: Int,
    val versionCode: Long,
    val displayName: String,
    val iconUrl: String?,
    val size: Long,
    var status: DownloadStatus,
    var progress: Int = 0,
    var speed: Long = 0L,
    var timeRemaining: Long = 0L,
    val apkUrl: String,
    val apkName: String,
    val hash: String,
    val hashType: String = HASH_SHA256,
    val signer: String?,
    val mirrors: List<String> = emptyList(),
    val archiveEntry: String? = null,
    val sigMd5: String? = null,
    val iconHash: String? = null,
    val downloadedAt: Long = 0L,
    val error: DownloadFailure? = null
) {
    val isFinished get() = status in DownloadStatus.finished
    val isRunning get() = status in DownloadStatus.running

    val isActive get() = isRunning || status == DownloadStatus.VERIFYING

    val isInstalling get() = status in DownloadStatus.installing

    companion object {
        const val HASH_SHA256 = "sha256"

        fun fromCatalog(app: AppEntity, candidate: AppCandidate): Download {
            // The candidate's package wins: a flavor installs under its own package,
            // and the install pipeline keys its state by that name.
            val packageName = candidate.packageName ?: app.packageName ?: app.slug
            return Download(
                packageName = packageName,
                repoId = NO_REPO_ID,
                versionCode = candidate.versionCode ?: app.versionCode ?: 0L,
                displayName = app.name.ifBlank { packageName },
                iconUrl = null,
                iconHash = app.iconHash,
                size = candidate.size ?: 0L,
                status = DownloadStatus.QUEUED,
                apkUrl = candidate.apkUrl,
                apkName = candidate.archiveEntry?.substringAfterLast('/')
                    ?: candidate.apkUrl.substringAfterLast('/'),
                hash = candidate.sha256.orEmpty(),
                hashType = HASH_SHA256,
                signer = candidate.sigSha256,
                sigMd5 = candidate.sigMd5,
                archiveEntry = candidate.archiveEntry,
                mirrors = emptyList(),
                downloadedAt = Date().time
            )
        }

        private const val NO_REPO_ID = -1
    }
}
