/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store.
 */

package me.timschneeberger.shizustore.compose.navigation

import me.timschneeberger.shizustore.data.model.AppSort

sealed class Destination {
    data class Main(val initialTab: Int = 0) : Destination()
    data class AppDetails(val packageName: String) : Destination()
    data class Permissions(val packageName: String) : Destination()
    data class MoreAbout(val packageName: String) : Destination()
    data object Downloads : Destination()
    data object Settings : Destination()

    data object AppearancePreferences : Destination()
    data object UpdatePreferences : Destination()
    data object InstallationPreferences : Destination()
    data object NetworkPreferences : Destination()
    data object ServerPreferences : Destination()
    data object PermissionPreferences : Destination()

    data object About : Destination()

    data object Installed : Destination()
    data object Favourites : Destination()

    data object Blacklist : Destination()
    data object IgnoredUpdates : Destination()

    data class AppList(
        val query: String = "",
        val categorySlug: String? = null,
        val recommended: Boolean = false,
        val sort: AppSort = AppSort.NAME
    ) : Destination()

    data object Back : Destination()
}
