/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.ItemSelection
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.viewmodel.SettingsViewModel

@Composable
fun AppearancePreferencesScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val themeStyle by viewModel.themeStyle.collectAsStateWithLifecycle()
    val dynamicColorsEnabled by viewModel.dynamicColorsEnabled.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_appearance_title),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(title = stringResource(R.string.settings_theme_title))

            ThemeOption.entries.forEach { option ->
                AuroraListItem(
                    headline = stringResource(option.labelRes),
                    leading = {
                        RadioButton(selected = themeStyle == option.value, onClick = null)
                    },
                    onClick = { viewModel.setThemeStyle(option.value) },
                    selection = ItemSelection.Radio(themeStyle == option.value)
                )
            }

            AuroraListItem(
                headline = stringResource(R.string.settings_dynamic_color_title),
                supporting = stringResource(R.string.settings_dynamic_color_subtitle),
                supportingMaxLines = 2,
                trailing = {
                    Switch(checked = dynamicColorsEnabled, onCheckedChange = null)
                },
                onClick = { viewModel.setDynamicColorsEnabled(!dynamicColorsEnabled) },
                selection = ItemSelection.Switch(dynamicColorsEnabled)
            )
        }
    }
}

private enum class ThemeOption(val value: Int, @StringRes val labelRes: Int) {
    SYSTEM(0, R.string.settings_theme_system),
    LIGHT(1, R.string.settings_theme_light),
    DARK(2, R.string.settings_theme_dark)
}
