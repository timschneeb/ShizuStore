/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import me.timschneeberger.shizustore.data.model.CategoryTag
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.model.flatten

/**
 * Builds the home CATEGORY rows from the top-level category tree. Membership
 * includes descendants, and the count comes from real members so a category the
 * self filter or the sync left empty never produces a header.
 */
object CategorySections {

    const val MIN_APPS = 4
    const val ITEM_LIMIT = 20

    fun build(
        categories: List<CategoryTag>,
        apps: List<ResolvedApp>,
        useInstallCounts: Boolean,
        minApps: Int = MIN_APPS,
        itemLimit: Int = ITEM_LIMIT
    ): List<AppGroup> {
        val byCategory = apps
            .filter { it.categorySlug != null }
            .groupBy { it.categorySlug.orEmpty() }

        return categories
            .map { root ->
                val members = listOf(root).flatten()
                    .flatMap { byCategory[it.slug].orEmpty() }
                root to members
            }
            .filter { (_, members) -> members.size >= minApps }
            .sortedWith(
                compareByDescending<Pair<CategoryTag, List<ResolvedApp>>> { it.second.size }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first.name }
            )
            .map { (root, members) ->
                AppGroup(
                    kind = AppGroupKind.CATEGORY,
                    apps = members.sortedWith(popularity(useInstallCounts)).take(itemLimit),
                    category = root.slug,
                    title = root.name
                )
            }
    }

    private fun popularity(useInstallCounts: Boolean): Comparator<ResolvedApp> {
        val primary = if (useInstallCounts) {
            compareByDescending<ResolvedApp> { it.installCount }
        } else {
            // Nulls last: unknown totals sink below known ones in the DESC list.
            compareByDescending<ResolvedApp> { it.downloadTotal ?: Long.MIN_VALUE }
        }
        return primary.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}
