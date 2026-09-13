/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose

fun indexUrl(repoAddress: String, path: String?): String? {
    val trimmed = path?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    return "${repoAddress.trimEnd('/')}/${trimmed.trimStart('/')}"
}
