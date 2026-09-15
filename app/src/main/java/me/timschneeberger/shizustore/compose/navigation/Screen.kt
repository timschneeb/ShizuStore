/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store.
 */

package me.timschneeberger.shizustore.compose.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import me.timschneeberger.shizustore.data.model.AppSort

@Parcelize
@Serializable
sealed class Screen : NavKey, Parcelable {

    @Serializable
    data class Main(val initialTab: Int = 0) : Screen()

    @Serializable
    data class AppDetails(val packageName: String) : Screen()

    @Serializable
    data class Permissions(val packageName: String) : Screen()

    @Serializable
    data class MoreAbout(val packageName: String) : Screen()

    @Serializable
    data object Downloads : Screen()

    @Serializable
    data object Settings : Screen()

    @Serializable
    data object AppearancePreferences : Screen()

    @Serializable
    data object UpdatePreferences : Screen()

    @Serializable
    data object InstallationPreferences : Screen()

    @Serializable
    data object NetworkPreferences : Screen()

    @Serializable
    data object ServerPreferences : Screen()

    @Serializable
    data object PermissionPreferences : Screen()

    @Serializable
    data object About : Screen()

    @Serializable
    data object Installed : Screen()

    @Serializable
    data object Favourites : Screen()

    @Serializable
    data object Blacklist : Screen()

    @Serializable
    data object IgnoredUpdates : Screen()

    @Serializable
    data class AppList(
        val query: String = "",
        val categorySlug: String? = null,
        val recommended: Boolean = false,
        val sort: AppSort = AppSort.NAME
    ) : Screen()
}
