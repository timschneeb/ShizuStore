/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2024-2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.theme

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.util.Preferences
import me.timschneeberger.shizustore.util.ThemePreference

@Composable
fun ShizuTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current

    val initialThemeStyle = remember(context) { ThemePreference.style(context) }
    val initialDynamicColor = remember(context) { ThemePreference.dynamicColors(context) }
    val initialBlackNight = remember(context) { ThemePreference.blackNight(context) }

    val themeStyleFlow = remember(context) {
        Preferences.integerFlow(
            context,
            Preferences.PREFERENCE_THEME_STYLE,
            ThemePreference.SYSTEM
        )
    }
    val dynamicColorFlow = remember(context) {
        Preferences.booleanFlow(
            context,
            Preferences.PREFERENCE_DYNAMIC_COLORS,
            Preferences.dynamicColorsDefault
        )
    }
    val blackNightFlow = remember(context) {
        Preferences.booleanFlow(context, Preferences.PREFERENCE_BLACK_NIGHT)
    }

    val themeStyle by themeStyleFlow.collectAsStateWithLifecycle(initialThemeStyle)
    val dynamicColor by dynamicColorFlow.collectAsStateWithLifecycle(initialDynamicColor)
    val blackNight by blackNightFlow.collectAsStateWithLifecycle(initialBlackNight)

    val useDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val lightScheme = if (useDynamicColor) {
        dynamicLightColorScheme(context)
    } else {
        BrandLightColorScheme
    }

    val darkScheme = if (useDynamicColor) {
        dynamicDarkColorScheme(context)
    } else {
        BrandDarkColorScheme
    }

    val darkTheme = when (themeStyle) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) {
        // Pure black base only affects night mode, never the light scheme.
        if (blackNight) darkScheme.withPureBlack() else darkScheme
    } else {
        lightScheme
    }

    val view = LocalView.current
    val activity = LocalActivity.current
    if (!view.isInEditMode) {
        SideEffect {
            val currentActivity = activity ?: return@SideEffect
            WindowCompat
                .getInsetsController(currentActivity.window, view)
                .apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }

            currentActivity.window.setBackgroundDrawable(
                colorScheme.background.toArgb().toDrawable()
            )
        }
    }

    MaterialExpressiveTheme(colorScheme = colorScheme, content = content)
}
