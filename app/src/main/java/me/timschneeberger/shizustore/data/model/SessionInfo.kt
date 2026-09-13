/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

data class SessionInfo(
    val sessionId: Int,
    val packageName: String,
    val versionCode: Long,
    val displayName: String = String()
)
