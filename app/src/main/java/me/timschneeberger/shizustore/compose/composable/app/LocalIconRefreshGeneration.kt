/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
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
 * Forces a fresh request only: the Coil memory key stays the URL, so loaded icons come from
 * cache without flicker while missing ones are retried.
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
