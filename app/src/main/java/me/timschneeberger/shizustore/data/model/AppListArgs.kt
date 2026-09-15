/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import kotlinx.serialization.Serializable

/** Sort presets for the shared app list. */
@Serializable
enum class AppSort { NAME, RECENTLY_ADDED, RECENTLY_UPDATED, STARS, DOWNLOADS, SIZE_DESC }

/** Price buckets. IAP and IAP_OR_PAID overlap by design; FREE is disjoint from both. */
@Serializable
enum class AppPrice { FREE, IAP, IAP_OR_PAID }

/**
 * One preset for the shared app list screen. Every entry point fills a single field:
 * the search field sets [query], a category tag sets [categorySlug], and the apps page
 * carousels set [sort]. The filters combine with AND.
 */
@Serializable
data class AppListArgs(
    val query: String = "",
    val categorySlug: String? = null,
    val price: AppPrice? = null,
    val recommended: Boolean = false,
    val sort: AppSort = AppSort.NAME
) {
    /** No filter is active, so the search tab shows history and the category tag cloud. */
    val isSearchHome: Boolean
        get() = query.isBlank() &&
            categorySlug == null &&
            price == null &&
            !recommended &&
            sort == AppSort.NAME
}
