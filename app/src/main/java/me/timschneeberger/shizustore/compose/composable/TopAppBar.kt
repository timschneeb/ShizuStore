/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2024-2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R

@Composable
fun TopAppBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    showNavigationIcon: Boolean = true,
    onNavigateBack: () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {}
) {
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
    TopAppBar(
        modifier = modifier,
        title = { if (title != null) Text(text = title) },
        navigationIcon = {
            if (showNavigationIcon) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_back)
                    )
                }
            }
        },
        colors = colors,
        actions = actions
    )
}
