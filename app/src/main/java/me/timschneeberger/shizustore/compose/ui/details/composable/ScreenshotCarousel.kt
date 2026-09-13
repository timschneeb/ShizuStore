/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import coil3.compose.AsyncImage
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.indexUrl
import me.timschneeberger.shizustore.data.model.AppDetails

@Composable
fun ScreenshotCarousel(details: AppDetails, modifier: Modifier = Modifier) {
    if (details.screenshots.isEmpty()) return

    LazyRow(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.spacing_small)),
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        items(details.screenshots) { path ->
            AsyncImage(
                model = indexUrl(details.repoAddress, path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(dimensionResource(R.dimen.screenshot_carousel_height))
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
            )
        }
    }
}
