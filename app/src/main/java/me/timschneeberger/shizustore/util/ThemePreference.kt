/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object ThemePreference {
    private const val TAG = "ThemePreference"

    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2

    @Volatile
    private var cachedStyle: Int? = null

    @Volatile
    private var cachedDynamicColors: Boolean? = null

    fun style(context: Context): Int {
        cachedStyle?.let { return it }

        val style = runBlocking {
            Preferences.readInteger(context, Preferences.PREFERENCE_THEME_STYLE, SYSTEM)
        }
        cachedStyle = style
        return style
    }

    fun dynamicColors(context: Context): Boolean {
        cachedDynamicColors?.let { return it }

        val enabled = runBlocking {
            Preferences.readBoolean(
                context,
                Preferences.PREFERENCE_DYNAMIC_COLORS,
                Preferences.dynamicColorsDefault
            )
        }
        cachedDynamicColors = enabled
        return enabled
    }

    fun withNightMode(base: Context): Context {
        val night = when (style(base)) {
            LIGHT -> Configuration.UI_MODE_NIGHT_NO
            DARK -> Configuration.UI_MODE_NIGHT_YES
            else -> return base
        }

        val configuration = Configuration(base.resources.configuration)
        if (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == night) return base

        configuration.uiMode =
            night or (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK)
        return base.createConfigurationContext(configuration)
    }

    suspend fun observe(context: Context): Unit = coroutineScope {
        launch {
            Preferences.integerFlow(context, Preferences.PREFERENCE_THEME_STYLE, SYSTEM)
                .distinctUntilChanged()
                .collect { style ->
                    cachedStyle = style
                    syncApplicationNightMode(context, style)
                }
        }

        launch {
            Preferences.booleanFlow(
                context,
                Preferences.PREFERENCE_DYNAMIC_COLORS,
                Preferences.dynamicColorsDefault
            ).distinctUntilChanged().collect { cachedDynamicColors = it }
        }
    }

    private fun syncApplicationNightMode(context: Context, style: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val mode = when (style) {
            LIGHT -> UiModeManager.MODE_NIGHT_NO
            DARK -> UiModeManager.MODE_NIGHT_YES
            else -> UiModeManager.MODE_NIGHT_AUTO
        }

        runCatching {
            context.getSystemService(UiModeManager::class.java).setApplicationNightMode(mode)
        }.onFailure {
            Log.w(TAG, "The system refused the per-app night mode", it)
        }
    }
}
