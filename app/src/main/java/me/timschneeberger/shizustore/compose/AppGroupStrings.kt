/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.viewmodel.AppGroup
import me.timschneeberger.shizustore.viewmodel.AppGroupKind

@Composable
fun appGroupTitle(kind: AppGroupKind, title: String? = null): String = when (kind) {
    AppGroupKind.RECOMMENDED -> stringResource(R.string.apps_recommended)
    AppGroupKind.RECENTLY_ADDED -> stringResource(R.string.apps_recently_added)
    AppGroupKind.RECENTLY_UPDATED -> stringResource(R.string.apps_recently_updated)
    AppGroupKind.MOST_STARRED -> stringResource(R.string.apps_most_starred)
    AppGroupKind.POPULAR -> stringResource(R.string.apps_popular)
    AppGroupKind.RANDOM_PICKS -> stringResource(R.string.apps_random_picks)
    AppGroupKind.CATEGORY -> title.orEmpty()
    AppGroupKind.USE_CASE -> title.orEmpty()
}

@Composable
fun appGroupTitle(group: AppGroup): String = appGroupTitle(group.kind, group.title)

@DrawableRes
fun appGroupIcon(group: AppGroup): Int = when (group.kind) {
    AppGroupKind.RECOMMENDED -> R.drawable.ic_editor_choice
    AppGroupKind.RECENTLY_ADDED -> R.drawable.ic_schedule
    AppGroupKind.RECENTLY_UPDATED -> R.drawable.ic_updates
    AppGroupKind.MOST_STARRED -> R.drawable.ic_star
    AppGroupKind.POPULAR -> R.drawable.ic_download_manager
    AppGroupKind.RANDOM_PICKS -> R.drawable.ic_redeem
    AppGroupKind.CATEGORY -> categoryIcon(group.category.orEmpty())
    AppGroupKind.USE_CASE -> R.drawable.ic_shizuku_icon
}
