/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's Permission model.
 */

package me.timschneeberger.shizustore.data.model

enum class PermissionType {
    INSTALL_UNKNOWN_APPS,
    POST_NOTIFICATIONS,
    DOZE_WHITELIST
}

data class Permission(
    val type: PermissionType,
    val title: String,
    val subtitle: String,
    val optional: Boolean,
    val isGranted: Boolean = false
)
