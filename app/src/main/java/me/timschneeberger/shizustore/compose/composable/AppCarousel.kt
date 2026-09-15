/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.appGroupTitle
import me.timschneeberger.shizustore.compose.composable.app.AppListItem
import me.timschneeberger.shizustore.compose.composable.app.AppTile
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.viewmodel.AppGroup
import me.timschneeberger.shizustore.viewmodel.AppGroupKind

@Composable
fun AppCarousel(
    groups: List<AppGroup>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    trailingSection: (@Composable () -> Unit)? = null,
    onGroupClick: (AppGroup) -> Unit = {},
    onAppClick: (ResolvedApp) -> Unit = {}
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        items(items = groups, key = { it.key }) { group ->
            Column {
                val openMore: (() -> Unit)? = if (group.kind.hasMorePage) {
                    { onGroupClick(group) }
                } else {
                    null
                }

                SectionHeader(title = appGroupTitle(group), onClick = openMore)
                if (group.kind.isTileStrip) {
                    AppTileStrip(apps = group.apps, onAppClick = onAppClick)
                } else {
                    AppCarouselStrip(
                        apps = group.apps,
                        showStars = group.kind == AppGroupKind.MOST_STARRED,
                        onAppClick = onAppClick
                    )
                }
            }
        }

        if (trailingSection != null) {
            item(key = "trailing") { trailingSection() }
        }
    }
}

/** A shelf of icons, for a group worth glancing along rather than reading. */
@Composable
fun AppTileStrip(apps: List<ResolvedApp>, onAppClick: (ResolvedApp) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        items(
            items = apps,
            key = { it.slug }
        ) { app ->
            AppTile(app = app, onClick = { onAppClick(app) })
        }
    }
}

@Composable
fun AppCarouselStrip(
    apps: List<ResolvedApp>,
    onAppClick: (ResolvedApp) -> Unit,
    modifier: Modifier = Modifier,
    showStars: Boolean = false
) {
    LazyHorizontalGrid(
        rows = GridCells.Fixed(CAROUSEL_GRID_ROWS),
        modifier = modifier.height(carouselGridHeight()),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        items(
            items = apps,
            key = { it.slug }
        ) { app ->
            AppListItem(
                app = app,
                onClick = { onAppClick(app) },
                modifier = Modifier.width(carouselItemWidth()),
                showStars = showStars
            )
        }
    }
}

@Composable
fun AppCarouselSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    groups: List<AppGroupKind> = AppGroupKind.entries.filter { it.isCarousel },
    cellCount: Int = DEFAULT_SKELETON_CELLS
) {
    val description = stringResource(R.string.loading)

    ShimmerHost {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .semantics { stateDescription = description },
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
        ) {
            items(items = groups, key = { it.name }) { kind ->
                Column {
                    ShimmerSectionHeader(clickable = kind.hasMorePage)
                    if (kind.isTileStrip) {
                        LazyRow(
                            contentPadding = PaddingValues(
                                horizontal = dimensionResource(R.dimen.spacing_large)
                            ),
                            horizontalArrangement = Arrangement.spacedBy(
                                dimensionResource(R.dimen.spacing_medium)
                            )
                        ) {
                            items(cellCount) { ShimmerAppTile() }
                        }
                    } else {
                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(CAROUSEL_GRID_ROWS),
                            modifier = Modifier.height(carouselGridHeight()),
                            horizontalArrangement = Arrangement.spacedBy(
                                dimensionResource(R.dimen.spacing_small)
                            ),
                            verticalArrangement = Arrangement.spacedBy(
                                dimensionResource(R.dimen.spacing_medium)
                            )
                        ) {
                            items(cellCount) {
                                Box(modifier = Modifier.width(carouselItemWidth())) {
                                    ShimmerAppRow()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun carouselItemWidth(): Dp = dimensionResource(R.dimen.carousel_item_width)

@Composable
private fun carouselGridHeight(): Dp {
    val rowHeight = dimensionResource(R.dimen.list_item_height_three_line)
    val rowGap = dimensionResource(R.dimen.spacing_medium)
    return rowHeight * CAROUSEL_GRID_ROWS + rowGap * (CAROUSEL_GRID_ROWS - 1)
}

private const val CAROUSEL_GRID_ROWS = 2

private const val DEFAULT_SKELETON_CELLS = 6
