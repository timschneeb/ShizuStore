/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import androidx.compose.runtime.Immutable
import me.timschneeberger.shizustore.data.model.ResolvedApp

enum class AppGroupKind(
    val isCarousel: Boolean,
    val hasMorePage: Boolean,
    val isTileStrip: Boolean = false
) {
    RECOMMENDED(isCarousel = true, hasMorePage = true, isTileStrip = true),
    RECENTLY_ADDED(isCarousel = true, hasMorePage = true, isTileStrip = true),
    RECENTLY_UPDATED(isCarousel = true, hasMorePage = true, isTileStrip = true),
    MOST_STARRED(isCarousel = true, hasMorePage = true),
    POPULAR(isCarousel = true, hasMorePage = true),
    RANDOM_PICKS(isCarousel = true, hasMorePage = false),
    CATEGORY(isCarousel = false, hasMorePage = true),
    USE_CASE(isCarousel = false, hasMorePage = true)
}

@Immutable
data class AppGroup(
    val kind: AppGroupKind,
    val apps: List<ResolvedApp>,
    val category: String? = null,
    /** Use case display name; [useCase] carries the slug for navigation. */
    val useCase: String? = null,
    /** Category display name; [category] carries the slug for navigation. */
    val title: String? = null
) {
    val key: String get() = "${kind.name}:${category.orEmpty()}:${useCase.orEmpty()}"
}
