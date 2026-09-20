/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's PermissionList.
 */

package me.timschneeberger.shizustore.compose.composable

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import me.timschneeberger.shizustore.BuildConfig
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.model.Permission
import me.timschneeberger.shizustore.data.model.PermissionType
import me.timschneeberger.shizustore.extensions.isTAndAbove

private const val TAG = "PermissionList"

@Composable
fun PermissionList(
    permissions: List<Permission>,
    modifier: Modifier = Modifier,
    onPermissionCallback: (PermissionType) -> Unit = {}
) {
    val context = LocalContext.current
    var requested by rememberSaveable { mutableStateOf<PermissionType?>(null) }

    @SuppressLint("BatteryLife", "InlinedApi")
    val intents = mapOf(
        PermissionType.INSTALL_UNKNOWN_APPS to Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${BuildConfig.APPLICATION_ID}".toUri()
        ),
        PermissionType.DOZE_WHITELIST to Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "package:${BuildConfig.APPLICATION_ID}".toUri()
        )
    )

    fun onResult() {
        requested?.let(onPermissionCallback)
    }

    val intentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { onResult() }
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onResult() }
    )

    fun request(type: PermissionType) {
        requested = type
        runCatching {
            when (type) {
                PermissionType.POST_NOTIFICATIONS ->
                    if (isTAndAbove) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }

                else -> intents[type]?.let(intentLauncher::launch)
            }
        }.onFailure {
            Log.e(TAG, "Could not request $type", it)
            requested = null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        permissions.groupBy { it.optional }
            .toSortedMap()
            .forEach { (optional, group) ->
                item(key = "divider-$optional") {
                    SectionHeader(
                        title = stringResource(
                            if (optional) R.string.item_optional else R.string.item_required
                        ),
                        titleColor = MaterialTheme.colorScheme.primary
                    )
                }

                items(items = group, key = { it.type.name }) { permission ->
                    PermissionListItem(
                        permission = permission,
                        onAction = { request(permission.type) }
                    )
                }
            }
    }
}

@Composable
private fun PermissionListItem(permission: Permission, onAction: () -> Unit) {
    AuroraListItem(
        headline = permission.title,
        supporting = permission.subtitle,
        supportingMaxLines = Int.MAX_VALUE,
        trailing = {
            TextButton(onClick = onAction, enabled = !permission.isGranted) {
                Text(
                    text = stringResource(
                        if (permission.isGranted) {
                            R.string.action_granted
                        } else {
                            R.string.action_grant
                        }
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    )
}
