/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import me.timschneeberger.shizustore.data.model.CategoryTag
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.model.SyntheticCategory
import me.timschneeberger.shizustore.data.model.flatten

/**
 * Builds the home CATEGORY rows from the top-level category tree. Membership
 * includes descendants, and the count comes from real members so a category the
 * self filter or the sync left empty never produces a header. The synthetic
 * Dhizuku-compatible section joins the same count-ordered list.
 */
object CategorySections {

    const val MIN_APPS = 4
    const val ITEM_LIMIT = 20

    fun build(
        categories: List<CategoryTag>,
        apps: List<ResolvedApp>,
        useInstallCounts: Boolean,
        dhizukuTitle: String,
        minApps: Int = MIN_APPS,
        itemLimit: Int = ITEM_LIMIT
    ): List<AppGroup> {
        val byCategory = apps
            .filter { it.categorySlug != null }
            .groupBy { it.categorySlug.orEmpty() }

        val realSections = categories
            .map { root ->
                Section(
                    slug = root.slug,
                    title = root.name,
                    members = listOf(root).flatten().flatMap { byCategory[it.slug].orEmpty() }
                )
            }
            .filter { it.members.size >= minApps }

        // Synthetic sections span every category; no minimum applies because
        // the flag itself is the curation.
        val dhizuku = apps.filter { it.dhizukuDeclared }
        val synthetic = buildList {
            if (dhizuku.isNotEmpty()) {
                add(Section(SyntheticCategory.DHIZUKU_SLUG, dhizukuTitle, dhizuku))
            }
        }

        return (realSections + synthetic)
            .sortedWith(
                compareByDescending<Section> { it.members.size }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            )
            .map { section ->
                AppGroup(
                    kind = AppGroupKind.CATEGORY,
                    apps = section.members.sortedWith(popularity(useInstallCounts)).take(itemLimit),
                    category = section.slug,
                    title = section.title
                )
            }
    }

    private data class Section(
        val slug: String,
        val title: String,
        val members: List<ResolvedApp>
    )

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
