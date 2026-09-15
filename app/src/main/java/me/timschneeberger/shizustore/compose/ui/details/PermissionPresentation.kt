/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import android.content.Context
import android.content.pm.PermissionInfo
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

internal enum class PermissionCategory { DANGEROUS, KNOWN, CUSTOM }

internal data class PermissionEntry(
    val label: String,
    val description: String?,
    val category: PermissionCategory,
    val icon: ImageBitmap? = null
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

@Suppress("DEPRECATION")
internal fun resolvePermission(context: Context, permission: String): PermissionEntry {
    val pm = context.packageManager
    val info = runCatching { pm.getPermissionInfo(permission, 0) }.getOrNull()
        ?: return PermissionEntry(
            label = customPermissionName(permission),
            description = permission,
            category = PermissionCategory.CUSTOM
        )

    return PermissionEntry(
        label = titleCase(info.loadLabel(pm).toString()),
        description = info.loadDescription(pm)?.toString()?.takeIf { it.isNotBlank() },
        category = if (isDangerousProtection(info.protectionLevel)) {
            PermissionCategory.DANGEROUS
        } else {
            PermissionCategory.KNOWN
        },
        icon = iconToImageBitmap(loadPermissionIcon(context, info))
    )
}

/**
 * The resource id is checked before loading because loadIcon() falls back to the
 * declaring app's icon and loadLogo() is almost always unset. Platform
 * permissions declare group UNDEFINED, so the platform table resolves them first.
 */
internal fun loadPermissionIcon(context: Context, info: PermissionInfo): Drawable? {
    val pm = context.packageManager
    if (info.icon != 0) {
        runCatching { info.loadUnbadgedIcon(pm) }.getOrNull()?.let { return it }
    }
    val groupName = PermissionGroups.groupOfPlatformPermission(info.name)
        ?: info.group?.takeUnless { it.endsWith(".UNDEFINED") }
        ?: return null
    val groupInfo = runCatching { pm.getPermissionGroupInfo(groupName, 0) }.getOrNull()
        ?: return null
    if (groupInfo.icon == 0) return null
    val resources = runCatching { pm.getResourcesForApplication(groupInfo.packageName) }
        .getOrNull() ?: return null
    return runCatching { resources.getDrawable(groupInfo.icon, context.theme) }.getOrNull()
        ?: runCatching { resources.getDrawable(groupInfo.icon, null) }.getOrNull()
}

@Suppress("DEPRECATION")
internal fun isDangerousProtection(protectionLevel: Int): Boolean =
    (protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS

private fun iconToImageBitmap(icon: Drawable?): ImageBitmap? =
    runCatching { icon?.toBitmap()?.asImageBitmap() }.getOrNull()
