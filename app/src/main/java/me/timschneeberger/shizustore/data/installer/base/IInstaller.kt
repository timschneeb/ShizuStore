/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's IInstaller (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.installer.base

import me.timschneeberger.shizustore.data.room.entity.Download

interface IInstaller {
    fun install(download: Download)
    fun removeFromInstallQueue(packageName: String)

    fun cancelInstall(packageName: String) {}
}
