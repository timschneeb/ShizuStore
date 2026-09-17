/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.installer

import android.content.Context

/** Shizuku being installed but not running still counts as installed; the card asks to start it. */
fun probeShizukuPrompt(context: Context): ShizukuPrompt = shizukuPrompt(
    available = ShizukuInstaller.isAvailable(context),
    running = ShizukuInstaller.isRunning(),
    permitted = ShizukuInstaller.hasPermission()
)
