/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.compose.composable.app.AppTile
import me.timschneeberger.shizustore.data.model.ResolvedApp

@Composable
fun DetailsCarousel(
    title: String,
    apps: List<ResolvedApp>,
    modifier: Modifier = Modifier,
    onHeaderClick: (() -> Unit)? = null,
    onAppClick: (ResolvedApp) -> Unit = {}
) {
    if (apps.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(title = title, onClick = onHeaderClick)
        LazyRow(
            contentPadding = PaddingValues(
                horizontal = dimensionResource(R.dimen.spacing_large)
            ),
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.spacing_medium)
            )
        ) {
            items(items = apps, key = { it.slug }) { app ->
                AppTile(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}
