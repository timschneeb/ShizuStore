/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.viewmodel.AppGroup
import me.timschneeberger.shizustore.viewmodel.AppGroupKind

@Composable
fun appGroupTitle(kind: AppGroupKind, category: String? = null): String = when (kind) {
    AppGroupKind.RECOMMENDED -> stringResource(R.string.apps_recommended)
    AppGroupKind.RECENTLY_ADDED -> stringResource(R.string.apps_recently_added)
    AppGroupKind.RECENTLY_UPDATED -> stringResource(R.string.apps_recently_updated)
    AppGroupKind.RANDOM_PICKS -> stringResource(R.string.apps_random_picks)
    AppGroupKind.CATEGORY -> category.orEmpty()
}

@Composable
fun appGroupTitle(group: AppGroup): String = appGroupTitle(group.kind, group.category)
