/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

/**
 * Client-side categories that have no row in the server category tree. Their
 * slugs are reserved: the query builder maps them to a flag filter and the UI
 * resolves their localized names.
 */
object SyntheticCategory {

    const val DHIZUKU_SLUG = "dhizuku"

    fun isDhizuku(slug: String?): Boolean = slug == DHIZUKU_SLUG

    /** Inserts the Dhizuku tag at its alphabetical slot so the filter sheet and
     * the search tag cloud offer it in line with the real categories. */
    fun insertDhizuku(categories: List<CategoryTag>, title: String): List<CategoryTag> =
        insert(categories, DHIZUKU_SLUG, title)

    private fun insert(
        categories: List<CategoryTag>,
        slug: String,
        title: String
    ): List<CategoryTag> {
        val tag = CategoryTag(slug = slug, name = title, appCount = 0)
        val index = categories.indexOfFirst { it.name > title }
        return if (index <
            0
        ) {
            categories + tag
        } else {
            categories.toMutableList().apply { add(index, tag) }
        }
    }
}
