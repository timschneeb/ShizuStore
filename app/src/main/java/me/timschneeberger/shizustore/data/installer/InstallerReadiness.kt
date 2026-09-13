/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.installer

import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.extensions.isOAndAbove

suspend fun canInstallUnattended(installer: Installer): Boolean = when (installer) {
    Installer.SESSION, Installer.NATIVE -> false
    Installer.ROOT -> RootInstaller.hasRootAccess()
    Installer.SHIZUKU ->
        isOAndAbove &&
            HiddenApiExemption.isPackageInstallerExempt &&
            ShizukuInstaller.hasPermission()
}
