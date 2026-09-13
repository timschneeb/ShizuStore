/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.SettingsApplications
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Rounded icon per server category slug. Unknown slugs fall back to the generic
 * category icon so categories added later still render.
 */
fun categoryIcon(slug: String): ImageVector = when (slug) {
    "ai-agents" -> Icons.Rounded.SmartToy
    "android-tv" -> Icons.Rounded.Tv
    "audio" -> Icons.Rounded.GraphicEq
    "automation" -> Icons.Rounded.Bolt
    "communication" -> Icons.Rounded.Forum
    "customization" -> Icons.Rounded.Palette
    "development-utilities" -> Icons.Rounded.Code
    "device-owner-dpm" -> Icons.Rounded.AdminPanelSettings
    "display-management" -> Icons.Rounded.Brightness6
    "entertainment" -> Icons.Rounded.Movie
    "file-management" -> Icons.Rounded.Folder
    "games" -> Icons.Rounded.SportsEsports
    "input-methods" -> Icons.Rounded.Keyboard
    "installer-app-stores" -> Icons.Rounded.Storefront
    "miscellaneous" -> Icons.Rounded.Category
    "network" -> Icons.Rounded.Lan
    "patching" -> Icons.Rounded.Handyman
    "power-management" -> Icons.Rounded.BatteryChargingFull
    "privacy" -> Icons.Rounded.PrivacyTip
    "productivity" -> Icons.Rounded.TaskAlt
    "quick-settings" -> Icons.Rounded.Tune
    "software-management" -> Icons.Rounded.SettingsApplications
    "task-manager" -> Icons.AutoMirrored.Rounded.ListAlt
    "terminals" -> Icons.Rounded.Terminal
    "vendor-specific" -> Icons.Rounded.Devices
    "vendor-specific-google-pixel" -> Icons.Rounded.PhoneAndroid
    "vendor-specific-miui" -> Icons.Rounded.PhoneAndroid
    "vendor-specific-samsung-oneui" -> Icons.Rounded.PhoneAndroid
    "vendor-specific-other" -> Icons.Rounded.DevicesOther
    else -> Icons.Rounded.Category
}
