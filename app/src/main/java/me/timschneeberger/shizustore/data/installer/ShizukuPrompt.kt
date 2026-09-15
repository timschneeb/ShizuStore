/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.installer

/** What the home Shizuku card should ask for; [READY] means there is nothing to show. */
enum class ShizukuPrompt { INSTALL, GRANT_PERMISSION, READY }

/** Pure mapping so the wording/handling split can be unit tested without Android. */
fun shizukuPrompt(available: Boolean, permitted: Boolean): ShizukuPrompt = when {
    !available -> ShizukuPrompt.INSTALL
    !permitted -> ShizukuPrompt.GRANT_PERMISSION
    else -> ShizukuPrompt.READY
}
