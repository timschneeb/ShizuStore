/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import java.util.concurrent.ConcurrentHashMap
import me.timschneeberger.shizustore.R

/**
 * Horizontal screenshot strip sourced from the server detail payload. URLs are
 * absolute upstream links, so they load directly; a tap opens a fullscreen
 * pager.
 */
@Composable
fun ScreenshotGallery(screenshots: List<String>, modifier: Modifier = Modifier) {
    if (screenshots.isEmpty()) return

    var viewerIndex by remember { mutableIntStateOf(-1) }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        itemsIndexed(screenshots) { index, url ->
            SubcomposeAsyncImage(
                model = rememberScreenshotModel(url),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                loading = {
                    // Fixed placeholder keeps the strip stable before the
                    // intrinsic (variable) width is known.
                    Box(
                        modifier = Modifier
                            .height(dimensionResource(R.dimen.screenshot_carousel_height))
                            .aspectRatio(9f / 16f),
                        contentAlignment = Alignment.Center
                    ) {
                        ScreenshotLoadingIndicator()
                    }
                },
                success = { SubcomposeAsyncImageContent() },
                modifier = Modifier
                    .height(dimensionResource(R.dimen.screenshot_carousel_height))
                    .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_medium)))
                    .clickable { viewerIndex = index }
            )
        }
    }

    if (viewerIndex >= 0) {
        ScreenshotViewer(
            screenshots = screenshots,
            initialIndex = viewerIndex,
            onDismiss = { viewerIndex = -1 }
        )
    }
}

@Composable
private fun ScreenshotViewer(screenshots: List<String>, initialIndex: Int, onDismiss: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { screenshots.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(state = pagerState) { page ->
                Box(
                    modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = rememberScreenshotModel(screenshots[page]),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        loading = { ScreenshotLoadingIndicator() },
                        success = { SubcomposeAsyncImageContent() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenshotLoadingIndicator() {
    val description = stringResource(R.string.loading)
    ContainedLoadingIndicator(
        modifier = Modifier
            .requiredSize(dimensionResource(R.dimen.icon_size_small))
            .semantics { stateDescription = description }
    )
}

/**
 * Screenshots change upstream, unlike immutable app icons, so they get their
 * own cache keys and never touch the shared disk cache: a fresh key every
 * 15 minutes lets the memory-held shot expire without growing the icon cache.
 */
@Composable
private fun rememberScreenshotModel(url: String): ImageRequest {
    val context = LocalPlatformContext.current
    val memoryKey = remember(url) { ScreenshotImageKeys.forUrl(url) }
    return remember(url, memoryKey) {
        ImageRequest.Builder(context)
            .data(url)
            .memoryCacheKey(memoryKey)
            // Dropping the disk cache also makes Coil send "no-cache, no-store",
            // so the long max-age headers cannot pin shots in the HTTP cache.
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }
}

private object ScreenshotImageKeys {
    private const val TTL_MS = 15L * 60L * 1000L
    private val fetchedAt = ConcurrentHashMap<String, Long>()

    fun forUrl(url: String): String {
        val now = System.currentTimeMillis()
        val last = fetchedAt[url]
        if (last == null || now - last >= TTL_MS) {
            fetchedAt[url] = now
            return "screenshot:$url@$now"
        }
        return "screenshot:$url@$last"
    }
}
