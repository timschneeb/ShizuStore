/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A server-curated Shizuku use case shown as a browse entry. */
@Entity(tableName = "use_case")
data class UseCaseEntity(
    @PrimaryKey val slug: String,
    val name: String = "",
    val appCount: Int = 0,
    val sortOrder: Int = 0
)
