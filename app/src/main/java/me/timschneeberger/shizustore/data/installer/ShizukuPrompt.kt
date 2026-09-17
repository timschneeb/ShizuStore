/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.installer

/** What the home Shizuku card should ask for; [READY] means there is nothing to show. */
enum class ShizukuPrompt { INSTALL, START, GRANT_PERMISSION, READY }

fun shizukuPrompt(available: Boolean, running: Boolean, permitted: Boolean): ShizukuPrompt = when {
    !available -> ShizukuPrompt.INSTALL
    // A stopped service reports no permission even when the grant still exists, so
    // starting it must come before granting.
    !running -> ShizukuPrompt.START
    !permitted -> ShizukuPrompt.GRANT_PERMISSION
    else -> ShizukuPrompt.READY
}
