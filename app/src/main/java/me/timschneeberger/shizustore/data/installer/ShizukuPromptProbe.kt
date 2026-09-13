/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.installer

import android.content.Context

/** Shizuku being installed but not running still counts as installed; the grant retries later. */
fun probeShizukuPrompt(context: Context): ShizukuPrompt = shizukuPrompt(
    available = ShizukuInstaller.isAvailable(context),
    permitted = ShizukuInstaller.hasPermission()
)
