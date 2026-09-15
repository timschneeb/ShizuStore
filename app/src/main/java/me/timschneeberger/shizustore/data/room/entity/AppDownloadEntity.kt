/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.timschneeberger.shizustore.data.api.SourceKind

/**
 * One installable candidate per (signing identity, ABI) (server `downloads[]`); `sigKey` mirrors the
 * server and carries the ABI suffix so per-architecture rows of one signing key stay distinct.
 */
@Entity(
    tableName = "app_download",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["slug"],
            childColumns = ["appSlug"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("appSlug"), Index(value = ["appSlug", "sigKey"], unique = true)]
)
data class AppDownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val appSlug: String,
    val source: SourceKind? = null,
    val apkUrl: String,
    val archiveEntry: String? = null,
    val versionCode: Long? = null,
    val versionName: String? = null,
    val size: Long? = null,
    val sha256: String? = null,
    val sigSha256: String? = null,
    val sigMd5: String? = null,
    val minSdk: Int? = null,
    val abi: String? = null,
    val isPrimary: Boolean = false,
    val sigKey: String
) {
    companion object {
        fun sigKeyOf(sigSha256: String?, sigMd5: String?, apkUrl: String, abi: String? = null): String {
            val base = firstToken(sigSha256)
                ?: firstToken(sigMd5)
                ?: "url:$apkUrl"
            return abi?.lowercase()?.ifBlank { null }?.let { "$base:$it" } ?: base
        }

        private fun firstToken(value: String?): String? =
            value?.trim()?.split(' ')?.firstOrNull()?.lowercase()?.ifBlank { null }
    }
}
