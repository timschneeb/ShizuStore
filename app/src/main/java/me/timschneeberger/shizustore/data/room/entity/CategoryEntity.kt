/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import me.timschneeberger.shizustore.data.api.CategorySection

/** Flattened server category tree (`/v1/categories`). */
@Entity(
    tableName = "category",
    indices = [Index("parentSlug"), Index("section")]
)
data class CategoryEntity(
    @PrimaryKey val slug: String,
    val name: String = "",
    val section: CategorySection = CategorySection.APPS,
    val parentSlug: String? = null,
    val appCount: Int = 0,
    val sortOrder: Int = 0
)

/** One breadcrumb entry in a detail response `categoryPath`. */
@Serializable
data class CategoryPath(
    val slug: String,
    val name: String = ""
)
