/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.BuildConfig
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.data.helper.SourceLauncher

@Composable
fun AboutScreen(modifier: Modifier = Modifier, onNavigateTo: (Destination) -> Unit = {}) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.title_about),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AuroraListItem(
                headline = stringResource(R.string.app_name),
                supporting = stringResource(
                    R.string.about_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                )
            )

            AuroraListItem(
                headline = stringResource(R.string.about_licence_title),
                supporting = stringResource(R.string.about_licence_value)
            )

            AboutLinkItem(
                headline = stringResource(R.string.about_developer_title),
                supporting = stringResource(R.string.about_developer_value),
                onClick = { SourceLauncher.open(context, DEVELOPER_URL) }
            )

            AboutLinkItem(
                headline = stringResource(R.string.about_auroradroid_title),
                supporting = stringResource(R.string.about_auroradroid_value),
                onClick = { SourceLauncher.open(context, AURORADROID_URL) }
            )
        }
    }
}

@Composable
private fun AboutLinkItem(headline: String, supporting: String, onClick: () -> Unit) {
    AuroraListItem(
        headline = headline,
        supporting = supporting,
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = Icons.Rounded.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

private const val DEVELOPER_URL = "https://github.com/timschneeb"
private const val AURORADROID_URL = "https://gitlab.com/AuroraOSS/auroradroid"
