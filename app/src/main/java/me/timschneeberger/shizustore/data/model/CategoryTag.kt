/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import me.timschneeberger.shizustore.data.room.entity.CategoryEntity

/**
 * A category as shown in the search tag cloud. A tag with [children] is a container
 * (for example Vendor-specific); it opens a menu instead of navigating directly.
 */
data class CategoryTag(
    val slug: String,
    val name: String,
    val appCount: Int,
    val children: List<CategoryTag> = emptyList()
) {
    val isContainer: Boolean get() = children.isNotEmpty()
}
object CategoryTagTree {
    fun build(categories: List<CategoryEntity>): List<CategoryTag> {
        val byParent = categories.groupBy { it.parentSlug }

        fun childrenOf(parentSlug: String?): List<CategoryTag> = byParent[parentSlug].orEmpty()
            .sortedWith(compareBy({ it.sortOrder }, { it.name }))
            .map { entity ->
                CategoryTag(
                    slug = entity.slug,
                    name = entity.name,
                    appCount = entity.appCount,
                    children = childrenOf(entity.slug)
                )
            }

        return childrenOf(null)
    }
}

/** Depth-first list of a tag tree, containers included. */
fun List<CategoryTag>.flatten(): List<CategoryTag> =
    flatMap { tag -> listOf(tag) + tag.children.flatten() }
