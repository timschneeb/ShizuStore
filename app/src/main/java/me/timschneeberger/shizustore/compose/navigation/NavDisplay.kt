/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store.
 */

package me.timschneeberger.shizustore.compose.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import me.timschneeberger.shizustore.BuildConfig
import me.timschneeberger.shizustore.compose.permission.rememberDozeExemptionRequest
import me.timschneeberger.shizustore.compose.ui.about.AboutScreen
import me.timschneeberger.shizustore.compose.ui.applist.AppListScreen
import me.timschneeberger.shizustore.compose.ui.blacklist.BlacklistScreen
import me.timschneeberger.shizustore.compose.ui.details.AppDetailsScreen
import me.timschneeberger.shizustore.compose.ui.details.ChangelogScreen
import me.timschneeberger.shizustore.compose.ui.details.MoreAboutScreen
import me.timschneeberger.shizustore.compose.ui.details.PermissionsScreen
import me.timschneeberger.shizustore.compose.ui.downloads.DownloadsScreen
import me.timschneeberger.shizustore.compose.ui.favourites.FavouritesScreen
import me.timschneeberger.shizustore.compose.ui.ignored.IgnoredUpdatesScreen
import me.timschneeberger.shizustore.compose.ui.installed.InstalledScreen
import me.timschneeberger.shizustore.compose.ui.main.MainScreen
import me.timschneeberger.shizustore.compose.ui.settings.AppearancePreferencesScreen
import me.timschneeberger.shizustore.compose.ui.settings.InstallationPreferencesScreen
import me.timschneeberger.shizustore.compose.ui.settings.NetworkPreferencesScreen
import me.timschneeberger.shizustore.compose.ui.settings.PermissionPreferencesScreen
import me.timschneeberger.shizustore.compose.ui.settings.ServerPreferencesScreen
import me.timschneeberger.shizustore.compose.ui.settings.SettingsScreen
import me.timschneeberger.shizustore.compose.ui.settings.UpdatePreferencesScreen
import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.viewmodel.DownloadActivityViewModel

private val navSlideSpec = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 380f,
    visibilityThreshold = IntOffset.VisibilityThreshold
)

private val navFadeSpec = spring<Float>(stiffness = 380f)

@Composable
fun ShizuNavDisplay(
    modifier: Modifier = Modifier,
    initialScreens: List<Screen> = listOf(Screen.Main()),
    downloadActivityViewModel: DownloadActivityViewModel = hiltViewModel()
) {
    val backStack = rememberNavBackStack(*initialScreens.toTypedArray())

    val isDownloading by downloadActivityViewModel.isDownloading.collectAsStateWithLifecycle()
    val requestDozeExemption = rememberDozeExemptionRequest()
    LaunchedEffect(isDownloading) { if (isDownloading) requestDozeExemption() }

    fun navigate(destination: Destination) {
        when (destination) {
            is Destination.Back -> if (backStack.size > 1) backStack.removeLastOrNull()
            is Destination.Main -> backStack.add(Screen.Main(destination.initialTab))
            is Destination.AppDetails -> backStack.add(Screen.AppDetails(destination.packageName))
            is Destination.Permissions ->
                backStack.add(Screen.Permissions(destination.packageName))
            is Destination.MoreAbout -> backStack.add(Screen.MoreAbout(destination.packageName))
            is Destination.Changelog -> backStack.add(Screen.Changelog(destination.packageName))
            is Destination.Downloads -> backStack.add(Screen.Downloads)
            is Destination.Settings -> backStack.add(Screen.Settings)
            is Destination.AppearancePreferences ->
                backStack.add(Screen.AppearancePreferences)
            is Destination.UpdatePreferences -> backStack.add(Screen.UpdatePreferences)
            is Destination.InstallationPreferences ->
                backStack.add(Screen.InstallationPreferences)
            is Destination.NetworkPreferences -> backStack.add(Screen.NetworkPreferences)
            is Destination.ServerPreferences ->
                // The server override is debug only.
                if (BuildConfig.DEBUG) backStack.add(Screen.ServerPreferences)
            is Destination.PermissionPreferences ->
                backStack.add(Screen.PermissionPreferences)
            is Destination.About -> backStack.add(Screen.About)
            is Destination.Installed -> backStack.add(Screen.Installed)
            is Destination.Favourites -> backStack.add(Screen.Favourites)
            is Destination.Blacklist -> backStack.add(Screen.Blacklist)
            is Destination.IgnoredUpdates -> backStack.add(Screen.IgnoredUpdates)
            is Destination.AppList ->
                backStack.add(
                    Screen.AppList(
                        query = destination.query,
                        categorySlug = destination.categorySlug,
                        recommended = destination.recommended,
                        sort = destination.sort
                    )
                )
        }
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        transitionSpec = {
            (slideInHorizontally(navSlideSpec) { it } + fadeIn(navFadeSpec)) togetherWith
                (slideOutHorizontally(navSlideSpec) { -it } + fadeOut(navFadeSpec))
        },
        popTransitionSpec = {
            (slideInHorizontally(navSlideSpec) { -it } + fadeIn(navFadeSpec)) togetherWith
                (slideOutHorizontally(navSlideSpec) { it } + fadeOut(navFadeSpec))
        },
        predictivePopTransitionSpec = {
            (slideInHorizontally(navSlideSpec) { -it } + fadeIn(navFadeSpec)) togetherWith
                (slideOutHorizontally(navSlideSpec) { it } + fadeOut(navFadeSpec))
        },
        entryProvider = entryProvider {
            entry<Screen.Main> { key ->
                MainScreen(initialTab = key.initialTab, onNavigateTo = ::navigate)
            }
            entry<Screen.AppDetails> { key ->
                AppDetailsScreen(packageName = key.packageName, onNavigateTo = ::navigate)
            }
            entry<Screen.Permissions> { key ->
                PermissionsScreen(packageName = key.packageName, onNavigateTo = ::navigate)
            }
            entry<Screen.MoreAbout> { key ->
                MoreAboutScreen(packageName = key.packageName, onNavigateTo = ::navigate)
            }
            entry<Screen.Changelog> { key ->
                ChangelogScreen(packageName = key.packageName, onNavigateTo = ::navigate)
            }
            entry<Screen.Downloads> {
                DownloadsScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.Settings> {
                SettingsScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.AppearancePreferences> {
                AppearancePreferencesScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.UpdatePreferences> {
                UpdatePreferencesScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.InstallationPreferences> {
                InstallationPreferencesScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.NetworkPreferences> {
                NetworkPreferencesScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.ServerPreferences> {
                ServerPreferencesScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.PermissionPreferences> {
                PermissionPreferencesScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.About> {
                AboutScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.Installed> {
                InstalledScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.Favourites> {
                FavouritesScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.Blacklist> {
                BlacklistScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.IgnoredUpdates> {
                IgnoredUpdatesScreen(onNavigateTo = ::navigate)
            }
            entry<Screen.AppList> { key ->
                AppListScreen(
                    args = AppListArgs(
                        query = key.query,
                        categorySlug = key.categorySlug,
                        recommended = key.recommended,
                        sort = key.sort
                    ),
                    showBack = true,
                    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                    onNavigateTo = ::navigate
                )
            }
        }
    )
}
