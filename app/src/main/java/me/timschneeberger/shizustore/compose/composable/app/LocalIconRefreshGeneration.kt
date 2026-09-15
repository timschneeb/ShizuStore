/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.request.ImageRequest

/** Bumped on every manual refresh so icons that failed once are retried. */
val LocalIconRefreshGeneration = compositionLocalOf { 0 }

/**
 * Only forces a fresh request so icons that failed once are retried. The Coil
 * memory key stays the URL, so loaded icons come from the memory cache without
 * flicker; missing ones hit the disk/network again.
 */
@Composable
fun rememberAppIconModel(url: String?): Any? {
    val context = LocalContext.current
    val generation = LocalIconRefreshGeneration.current
    return remember(url, generation) {
        if (url.isNullOrEmpty()) {
            null
        } else {
            ImageRequest.Builder(context)
                .data(url)
                .memoryCacheKey(url)
                .build()
        }
    }
}
