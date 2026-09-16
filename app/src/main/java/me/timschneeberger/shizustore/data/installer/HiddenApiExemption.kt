/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.installer

object HiddenApiExemption {

    @Volatile
    private var exempted = true

    val isPackageInstallerExempt: Boolean
        get() = exempted

    fun record(exempted: Boolean) {
        this.exempted = exempted
    }
}
