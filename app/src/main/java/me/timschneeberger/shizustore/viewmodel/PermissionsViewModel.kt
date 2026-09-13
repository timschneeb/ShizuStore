/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.viewmodel

import android.Manifest
import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.model.Permission
import me.timschneeberger.shizustore.data.model.PermissionType
import me.timschneeberger.shizustore.extensions.checkManifestPermission
import me.timschneeberger.shizustore.extensions.isIgnoringBatteryOptimizations
import me.timschneeberger.shizustore.extensions.isOAndAbove
import me.timschneeberger.shizustore.extensions.isTAndAbove

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _permissions = MutableStateFlow(buildPermissions())
    val permissions: StateFlow<List<Permission>> = _permissions.asStateFlow()

    fun refreshPermissions() {
        _permissions.value = buildPermissions()
    }

    private fun buildPermissions(): List<Permission> = buildList {
        add(
            Permission(
                type = PermissionType.INSTALL_UNKNOWN_APPS,
                title = context.getString(R.string.permission_install),
                subtitle = context.getString(R.string.permission_install_desc),
                optional = false,
                isGranted = isGranted(PermissionType.INSTALL_UNKNOWN_APPS)
            )
        )
        if (isTAndAbove) {
            add(
                Permission(
                    type = PermissionType.POST_NOTIFICATIONS,
                    title = context.getString(R.string.permission_notifications),
                    subtitle = context.getString(
                        R.string.permission_notifications_desc
                    ),
                    optional = true,
                    isGranted = isGranted(PermissionType.POST_NOTIFICATIONS)
                )
            )
        }
        add(
            Permission(
                type = PermissionType.DOZE_WHITELIST,
                title = context.getString(R.string.permission_doze),
                subtitle = context.getString(R.string.permission_doze_desc),
                optional = true,
                isGranted = isGranted(PermissionType.DOZE_WHITELIST)
            )
        )
    }

    private fun isGranted(type: PermissionType): Boolean = when (type) {
        PermissionType.INSTALL_UNKNOWN_APPS ->
            !isOAndAbove || context.packageManager.canRequestPackageInstalls()

        PermissionType.POST_NOTIFICATIONS ->
            !isTAndAbove || context.checkManifestPermission(Manifest.permission.POST_NOTIFICATIONS)

        PermissionType.DOZE_WHITELIST -> context.isIgnoringBatteryOptimizations()
    }
}
