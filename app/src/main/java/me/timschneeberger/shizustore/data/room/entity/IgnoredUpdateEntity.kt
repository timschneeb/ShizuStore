/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ignored_update")
data class IgnoredUpdateEntity(
    @PrimaryKey val packageName: String,
    val versionCode: Long?
)
