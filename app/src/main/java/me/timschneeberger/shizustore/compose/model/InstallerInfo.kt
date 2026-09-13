/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.model

import androidx.annotation.StringRes
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.model.Installer

data class InstallerInfo(
    val installer: Installer,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @StringRes val description: Int
)

object InstallerCatalogue {

    fun infoFor(installer: Installer): InstallerInfo = when (installer) {
        Installer.SESSION -> InstallerInfo(
            installer = Installer.SESSION,
            title = R.string.installer_session_title,
            subtitle = R.string.installer_session_subtitle,
            description = R.string.installer_session_desc
        )

        Installer.NATIVE -> InstallerInfo(
            installer = Installer.NATIVE,
            title = R.string.installer_native_title,
            subtitle = R.string.installer_native_subtitle,
            description = R.string.installer_native_desc
        )

        Installer.ROOT -> InstallerInfo(
            installer = Installer.ROOT,
            title = R.string.installer_root_title,
            subtitle = R.string.installer_root_subtitle,
            description = R.string.installer_root_desc
        )

        Installer.SHIZUKU -> InstallerInfo(
            installer = Installer.SHIZUKU,
            title = R.string.installer_shizuku_title,
            subtitle = R.string.installer_shizuku_subtitle,
            description = R.string.installer_shizuku_desc
        )
    }
}
