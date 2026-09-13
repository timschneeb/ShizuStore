/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.compose.composable.app.LocalIconRefreshGeneration
import me.timschneeberger.shizustore.compose.navigation.Screen
import me.timschneeberger.shizustore.compose.navigation.ShizuNavDisplay
import me.timschneeberger.shizustore.compose.theme.ShizuTheme
import me.timschneeberger.shizustore.compose.ui.main.MainTab
import me.timschneeberger.shizustore.data.api.BaseUrlProvider
import me.timschneeberger.shizustore.data.helper.InstallReconciler
import me.timschneeberger.shizustore.data.helper.SyncHelper
import me.timschneeberger.shizustore.data.repository.InstalledRepository
import me.timschneeberger.shizustore.util.ServerConfig
import me.timschneeberger.shizustore.util.ThemePreference

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncHelper: SyncHelper

    @Inject
    lateinit var installedRepository: InstalledRepository

    @Inject
    lateinit var installReconciler: InstallReconciler

    @Inject
    lateinit var baseUrlProvider: BaseUrlProvider

    @VisibleForTesting
    internal var initialTab: Int = TAB_APPS
        private set

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ThemePreference.withNightMode(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initialTab = intent?.getIntExtra(EXTRA_INITIAL_TAB, TAB_APPS) ?: TAB_APPS

        val screens = initialScreens(intent, initialTab)

        setContent {
            val iconGeneration by syncHelper.iconGeneration
                .collectAsStateWithLifecycle(initialValue = 0)
            CompositionLocalProvider(LocalIconRefreshGeneration provides iconGeneration) {
                ShizuTheme {
                    ShizuNavDisplay(modifier = Modifier.fillMaxSize(), initialScreens = screens)
                }
            }
        }

        lifecycleScope.launch { ThemePreference.observe(this@MainActivity) }

        lifecycleScope.launch {
            baseUrlProvider.observe().collect { ServerConfig.baseUrl = it }
        }

        lifecycleScope.launch {
            runCatching { installReconciler.reconcileOnLaunch() }
                .onFailure { Log.w(TAG, "Could not run the launch reconciliation", it) }
        }

        lifecycleScope.launch {
            installedRepository.refreshAll()
            syncHelper.sync()
        }
    }

    private fun initialScreens(intent: Intent?, tab: Int): List<Screen> {
        val target = intent
            ?.let { IntentCompat.getParcelableExtra(it, EXTRA_SCREEN, Screen::class.java) }
            ?.takeIf { it !is Screen.Main }

        return listOfNotNull(Screen.Main(tab), target)
    }

    companion object {

        private const val TAG = "MainActivity"

        const val EXTRA_INITIAL_TAB = "me.timschneeberger.shizustore.extra.INITIAL_TAB"

        const val EXTRA_SCREEN = "me.timschneeberger.shizustore.extra.SCREEN"

        val TAB_APPS = MainTab.APPS.ordinal

        val TAB_UPDATES = MainTab.UPDATES.ordinal
    }
}
