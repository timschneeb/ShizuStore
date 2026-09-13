/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import android.content.pm.PackageManager

internal data class PermissionEntry(
    val label: String,
    val description: String?,
    val isKnown: Boolean
)

private val MINOR_WORDS = setOf(
    "a", "an", "and", "as", "at", "but", "by", "for", "from", "in",
    "nor", "of", "on", "or", "the", "to", "with"
)

private fun capitalizeWord(word: String): String {
    if (word.any { it.isUpperCase() }) return word

    val index = word.indexOfFirst { it.isLetter() }
    if (index < 0) return word

    return word.substring(0, index) + word[index].uppercaseChar() + word.substring(index + 1)
}

internal fun titleCase(text: String): String = text
    .split(' ')
    .mapIndexed { index, word ->
        if (index > 0 && word.lowercase() in MINOR_WORDS) word else capitalizeWord(word)
    }
    .joinToString(" ")

internal fun customPermissionName(permission: String): String =
    titleCase(permission.substringAfterLast('.').replace('_', ' ').lowercase())

internal fun resolvePermission(pm: PackageManager, permission: String): PermissionEntry {
    val info = runCatching { pm.getPermissionInfo(permission, 0) }.getOrNull()
        ?: return PermissionEntry(
            label = customPermissionName(permission),
            description = permission,
            isKnown = false
        )

    return PermissionEntry(
        label = titleCase(info.loadLabel(pm).toString()),
        description = info.loadDescription(pm)?.toString()?.takeIf { it.isNotBlank() },
        isKnown = true
    )
}
