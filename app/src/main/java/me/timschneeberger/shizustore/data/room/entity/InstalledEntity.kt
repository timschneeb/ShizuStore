/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "installed")
data class InstalledEntity(
    @PrimaryKey val packageName: String,
    val versionCode: Long,
    val versionName: String,
    val signer: String?,
    val signerMd5: String? = null
)
