/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.theme.motionSpatialSpec

/**
 * Horizontal screenshot strip sourced from the server detail payload. URLs are
 * absolute upstream links, so they load directly; a tap opens a fullscreen
 * pager.
 */
@Composable
fun ScreenshotGallery(screenshots: List<String>, modifier: Modifier = Modifier) {
    if (screenshots.isEmpty()) return

    var viewerIndex by remember { mutableIntStateOf(-1) }

    val stripHeight = dimensionResource(R.dimen.screenshot_carousel_height)
    val cornerRadius = dimensionResource(R.dimen.radius_medium)
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    // Bound the strip decode by the rendered height instead of handing Coil a
    // constraints resolver, which forces SubcomposeAsyncImage into its
    // SubcomposeLayout slow path.
    val stripPixelHeight = with(LocalDensity.current) { stripHeight.roundToPx() }
    val stripSize = remember(stripPixelHeight) {
        Size(width = stripPixelHeight * 2, height = stripPixelHeight)
    }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = dimensionResource(R.dimen.spacing_large),
            vertical = 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        itemsIndexed(
            items = screenshots,
            key = { _, url -> url },
            contentType = { _, _ -> "screenshot" }
        ) { index, url ->
            SubcomposeAsyncImage(
                model = rememberScreenshotModel(url, stripSize),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                loading = {
                    // Fixed placeholder keeps the strip stable before the
                    // intrinsic (variable) width is known.
                    Box(
                        modifier = Modifier
                            .height(stripHeight)
                            .aspectRatio(9f / 16f),
                        contentAlignment = Alignment.Center
                    ) {
                        ScreenshotLoadingIndicator()
                    }
                },
                success = { SubcomposeAsyncImageContent() },
                modifier = Modifier
                    .height(stripHeight)
                    .clip(shape)
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
    val scope = rememberCoroutineScope()
    val closeFocusRequester = remember { FocusRequester() }
    // A zoomed shot pans with one finger, so paging must yield until a pinch
    // returns the shot to 1x.
    var pageZoomed by remember { mutableStateOf(false) }

    val buttonColors = IconButtonDefaults.iconButtonColors(
        contentColor = Color.White,
        disabledContentColor = Color.White.copy(alpha = 0.4f)
    )
    val scrim = Color.Black.copy(alpha = OVERLAY_SCRIM_ALPHA)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !pageZoomed
            ) { page ->
                ZoomableScreenshot(
                    url = screenshots[page],
                    isActive = pagerState.currentPage == page,
                    onZoomChanged = { pageZoomed = it }
                )
            }

            // Explicit controls keep the viewer navigable with a remote; the
            // image itself is not a focus target.
            IconButton(
                onClick = onDismiss,
                colors = buttonColors,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(dimensionResource(R.dimen.spacing_large))
                    .background(scrim, CircleShape)
                    .focusRequester(closeFocusRequester)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_clear),
                    contentDescription = stringResource(R.string.action_close)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = dimensionResource(R.dimen.spacing_large))
                    .background(scrim, CircleShape),
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_small)
                )
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    enabled = pagerState.currentPage > 0,
                    colors = buttonColors
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = stringResource(R.string.action_previous),
                        modifier = Modifier.graphicsLayer { scaleX = -1f }
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    enabled = pagerState.currentPage < screenshots.size - 1,
                    colors = buttonColors
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = stringResource(R.string.action_next)
                    )
                }
            }
        }

        LaunchedEffect(Unit) { closeFocusRequester.requestFocus() }
    }
}

/**
 * Pinch to zoom, with one-finger panning while zoomed. Single-finger drags at
 * 1x are left unconsumed so the surrounding pager can still page between shots.
 */
@Composable
private fun ZoomableScreenshot(
    url: String,
    isActive: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val scope = rememberCoroutineScope()
    val scaleResetSpec = motionSpatialSpec<Float>()
    val offsetResetSpec = motionSpatialSpec<Offset>()

    // Spring back to rest when the page scrolls away. The launch lives in the
    // screen scope, not this effect, so a quick swipe back does not cancel it
    // with the zoom still applied.
    LaunchedEffect(isActive) {
        if (!isActive && (scale != 1f || offset != Offset.Zero)) {
            onZoomChanged(false)
            val startScale = scale
            val startOffset = offset
            scope.launch {
                animate(
                    initialValue = startScale,
                    targetValue = 1f,
                    animationSpec = scaleResetSpec
                ) { value, _ -> scale = value }
            }
            scope.launch {
                animate(
                    typeConverter = Offset.VectorConverter,
                    initialValue = startOffset,
                    targetValue = Offset.Zero,
                    animationSpec = offsetResetSpec
                ) { value, _ -> offset = value }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val pinching = event.changes.size > 1 &&
                            (zoomChange != 1f || panChange != Offset.Zero)
                        if (pinching || scale > 1f) {
                            val newScale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
                            val maxX = (newScale - 1f) / 2f * containerSize.width
                            val maxY = (newScale - 1f) / 2f * containerSize.height
                            scale = newScale
                            offset = Offset(
                                x = (offset.x + panChange.x).coerceIn(-maxX, maxX),
                                y = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                            )
                            event.changes.forEach { if (it.pressed) it.consume() }
                            onZoomChanged(newScale > 1f)
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = rememberScreenshotModel(url, Size.ORIGINAL),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            loading = { ScreenshotLoadingIndicator() },
            success = { SubcomposeAsyncImageContent() },
            modifier = Modifier.fillMaxSize()
        )
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
private fun rememberScreenshotModel(url: String, size: Size): ImageRequest {
    val context = LocalPlatformContext.current
    val memoryKey = remember(url) { ScreenshotImageKeys.forUrl(url) }
    return remember(url, memoryKey, size) {
        ImageRequest.Builder(context)
            .data(url)
            .size(size)
            // The size is part of the key so the fullscreen viewer does not
            // reuse the smaller strip decode.
            .memoryCacheKey("$memoryKey:${size.width}x${size.height}")
            // Dropping the disk cache also makes Coil send "no-cache, no-store",
            // so the long max-age headers cannot pin shots in the HTTP cache.
            .diskCachePolicy(CachePolicy.DISABLED)
            .build()
    }
}

private const val OVERLAY_SCRIM_ALPHA = 0.4f
private const val MAX_ZOOM = 5f

/**
 * Bounded LRU: the strip only holds a handful of shots, so an unbounded map
 * would leak one entry per screenshot ever seen.
 */
private object ScreenshotImageKeys {
    private const val TTL_MS = 15L * 60L * 1000L
    private const val MAX_ENTRIES = 64

    private val fetchedAt = object : LinkedHashMap<String, Long>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>): Boolean =
            size > MAX_ENTRIES
    }

    fun forUrl(url: String): String = synchronized(fetchedAt) {
        val now = System.currentTimeMillis()
        val last = fetchedAt[url]
        if (last == null || now - last >= TTL_MS) {
            fetchedAt[url] = now
            "screenshot:$url@$now"
        } else {
            "screenshot:$url@$last"
        }
    }
}
