/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose

import androidx.annotation.DrawableRes
import me.timschneeberger.shizustore.R

/**
 * Unknown slugs fall back to the generic category icon so categories added later still render.
 */
@DrawableRes
fun categoryIcon(slug: String): Int = when (slug) {
    "ai-agents" -> R.drawable.ic_smart_toy
    "android-tv" -> R.drawable.ic_tv
    "audio" -> R.drawable.ic_graphic_eq
    "automation" -> R.drawable.ic_precision_manufacturing
    "communication" -> R.drawable.ic_forum
    "customization" -> R.drawable.ic_palette
    "development-utilities" -> R.drawable.ic_code
    "device-owner-dpm" -> R.drawable.ic_shield_person
    "display-management" -> R.drawable.ic_display_settings
    "entertainment" -> R.drawable.ic_movie
    "file-management" -> R.drawable.ic_folder
    "games" -> R.drawable.ic_sports_esports
    "input-methods" -> R.drawable.ic_keyboard
    "installer-app-stores" -> R.drawable.ic_storefront
    "miscellaneous" -> R.drawable.ic_category
    "network" -> R.drawable.ic_android_wifi_3_bar
    "patching" -> R.drawable.ic_handyman
    "power-management" -> R.drawable.ic_charger
    "privacy" -> R.drawable.ic_encrypted
    "productivity" -> R.drawable.ic_task_alt
    "quick-settings" -> R.drawable.ic_tune
    "software-management" -> R.drawable.ic_deployed_code
    "task-manager" -> R.drawable.ic_memory
    "terminals" -> R.drawable.ic_terminal_2
    "vendor-specific" -> R.drawable.ic_devices
    "vendor-specific-google-pixel" -> R.drawable.ic_mobile_2
    "vendor-specific-miui" -> R.drawable.ic_mobile_2
    "vendor-specific-samsung-oneui" -> R.drawable.ic_mobile_2
    "vendor-specific-other" -> R.drawable.ic_devices_other
    else -> R.drawable.ic_category
}
