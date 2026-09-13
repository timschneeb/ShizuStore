/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's PermissionRationaleScreen.
 */

package me.timschneeberger.shizustore.compose.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.PermissionList
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.viewmodel.PermissionsViewModel

@Composable
fun PermissionPreferencesScreen(
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.settings_permissions_title),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        PermissionList(
            permissions = permissions,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            onPermissionCallback = { viewModel.refreshPermissions() }
        )
    }
}
