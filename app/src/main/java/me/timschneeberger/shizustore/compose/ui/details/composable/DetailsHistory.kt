/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.api.AppHistoryDto
import me.timschneeberger.shizustore.data.api.InstallDayDto
import me.timschneeberger.shizustore.data.api.StarDayDto
import me.timschneeberger.shizustore.util.CommonUtil

/** Chart modes the section can show; a mode only appears when its series has signal. */
private enum class HistoryMetric(val iconRes: Int) {
    INSTALLS(R.drawable.ic_download_manager),
    STARS(R.drawable.ic_star)
}

/**
 * Activity charts under the stats strip. One GET /v1/apps/{slug}/history
 * fetch (365 days) backs both modes: installs plot the daily series, stars
 * plot the new stars received per week (derived from the daily levels the
 * API carries forward). Both modes only appear once their series has enough
 * signal to draw (two recorded install days, two star levels). The section
 * grows in when signal exists so the rows below slide instead of the block
 * popping in.
 */
@Composable
fun DetailsHistory(history: AppHistoryDto?, modifier: Modifier = Modifier) {
    val installs = history?.installs.orEmpty()
    val stars = history?.stars.orEmpty()

    // A single recorded day is a lone dot, so wait for a second one.
    val hasInstalls = installs.count { it.count > 0 } >= 2
    // Weekly gains need at least two daily levels to produce one bucket.
    val hasStars = stars.size >= 2

    AnimatedVisibility(
        visible = hasInstalls || hasStars,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        modifier = modifier
    ) {
        StatisticsBody(installs, stars, hasInstalls, hasStars)
    }
}

@Composable
private fun StatisticsBody(
    installs: List<InstallDayDto>,
    stars: List<StarDayDto>,
    hasInstalls: Boolean,
    hasStars: Boolean
) {
    val available = buildList {
        if (hasInstalls) add(HistoryMetric.INSTALLS)
        if (hasStars) add(HistoryMetric.STARS)
    }

    // The selection survives recomposition; data landing later can only widen
    // the set, so a saved mode stays valid once chosen.
    var savedMetric by rememberSaveable { mutableStateOf("") }
    val fallback = if (hasInstalls) HistoryMetric.INSTALLS else HistoryMetric.STARS
    val mode = available.firstOrNull { it.name == savedMetric } ?: fallback

    // Weekly star gains parse every date in the window, so all three
    // derivations stay memoized; only a new series or a mode switch
    // recomputes them.
    val series: List<Pair<String, Long>> = remember(installs, stars, mode) {
        when (mode) {
            HistoryMetric.INSTALLS ->
                installs.map { it.day to it.count }

            HistoryMetric.STARS ->
                weeklyStarGains(stars)
        }
    }
    if (series.isEmpty()) return
    val values = remember(series) { series.map { it.second } }

    val title = stringResource(R.string.details_history_title)
    val metricLabel = stringResource(
        when (mode) {
            HistoryMetric.INSTALLS -> R.string.details_history_installs
            HistoryMetric.STARS -> R.string.details_stats_stars
        }
    )
    val chartDescription = if (mode == HistoryMetric.STARS) {
        stringResource(R.string.details_history_weekly_desc, metricLabel, series.size)
    } else {
        stringResource(R.string.details_history_chart_desc, metricLabel, series.size)
    }
    // Bottom axis label names the bucket size; the right-hand summary always
    // reports the most recent week so the two sides never drift apart. The
    // tooltip reuses it as its value unit ("45 stars per week").
    val axisLabel = stringResource(
        when (mode) {
            HistoryMetric.INSTALLS -> R.string.details_history_installs_axis
            HistoryMetric.STARS -> R.string.details_history_stars_axis
        }
    )
    val lastWeek = when (mode) {
        HistoryMetric.INSTALLS -> values.takeLast(7).sum()
        HistoryMetric.STARS -> values.last()
    }
    val summary = stringResource(
        when (mode) {
            HistoryMetric.INSTALLS -> R.string.details_history_downloads_last_week
            HistoryMetric.STARS -> R.string.details_history_stars_last_week
        },
        CommonUtil.formatCount(lastWeek)
    )

    // Cumulative level at each point: downloads accumulate the daily series,
    // stars read the level snapshotted on that day (weekly buckets carry the
    // Sunday's level, before the first snapshot counts as zero).
    val totals: List<Long> = remember(series, mode, stars) {
        when (mode) {
            HistoryMetric.INSTALLS -> {
                var running = 0L
                values.map {
                    running += it
                    running
                }
            }

            HistoryMetric.STARS -> {
                val levels = stars
                    .map { LocalDate.parse(it.day) to it.stars.toLong() }
                    .sortedBy { (day, _) -> day }
                series.map { (label, _) ->
                    val bucketStart = LocalDate.parse(label)
                    levels.lastOrNull { (day, _) -> day <= bucketStart }?.second ?: 0L
                }
            }
        }
    }

    // The chart owns its scrub state so moving a finger across it never
    // recomputes the series or the rows above; this body only rebuilds when
    // the history data or the selected mode changes.
    val totalTemplate = stringResource(
        when (mode) {
            HistoryMetric.INSTALLS -> R.string.details_history_total_downloads
            HistoryMetric.STARS -> R.string.details_history_total_stars
        }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_small)
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (available.size > 1) {
                SingleChoiceSegmentedButtonRow {
                    available.forEachIndexed { index, candidate ->
                        val label = stringResource(
                            when (candidate) {
                                HistoryMetric.INSTALLS -> R.string.details_history_installs
                                HistoryMetric.STARS -> R.string.details_stats_stars
                            }
                        )
                        // SegmentedButton sizes itself from the label slot only,
                        // so the icon has to travel as the label or it lands in
                        // a zero-height slot and renders blank.
                        SegmentedButton(
                            selected = mode == candidate,
                            onClick = { savedMetric = candidate.name },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = available.size
                            ),
                            icon = {},
                            label = {
                                Icon(
                                    painter = painterResource(candidate.iconRes),
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.semantics { contentDescription = label }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
        StatisticsChart(
            values = values,
            series = series,
            totals = totals,
            axisLabel = axisLabel,
            totalTemplate = totalTemplate,
            chartDescription = chartDescription
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = axisLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The 72dp chart row: sparkline plus the scrub popover. The scrub state
 * lives here so finger movement recomposes only this node (and only the
 * tooltip text), never the section body above it.
 */
@Composable
private fun StatisticsChart(
    values: List<Long>,
    series: List<Pair<String, Long>>,
    totals: List<Long>,
    axisLabel: String,
    totalTemplate: String,
    chartDescription: String
) {
    var scrubIndex by remember { mutableStateOf<Int?>(null) }
    val scrubAnchorX = remember { mutableStateOf(0f) }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL) }
    val tooltipText = scrubIndex?.coerceIn(0, values.lastIndex)?.let { idx ->
        LocalDate.parse(series[idx].first).format(dateFormatter) +
            "\n" + CommonUtil.formatCount(values[idx]) +
            " " + axisLabel.lowercase(Locale.ROOT) +
            "\n" + totalTemplate.format(CommonUtil.formatCount(totals[idx]))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
    ) {
        HistorySparkline(
            values = values,
            contentDescription = chartDescription,
            selectedIndex = scrubIndex,
            onScrub = { index, pointX ->
                scrubIndex = index
                if (index != null) scrubAnchorX.value = pointX
            },
            modifier = Modifier.fillMaxSize()
        )
        // The tooltip lives outside the chart canvas so it can never sit
        // under the finger; Box does not clip, so it floats over the
        // neighbouring rows like a popover.
        tooltipText?.let { text ->
            ChartTooltip(
                text = text,
                anchorX = scrubAnchorX.value,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

/**
 * Daily star levels become new stars per week: consecutive deltas bucketed
 * by Sunday week start (matching how the server's feed groups its gains).
 * Levels can dip when someone unstars, so a bucket may go negative.
 */
private fun weeklyStarGains(stars: List<StarDayDto>): List<Pair<String, Long>> = stars
    .map { LocalDate.parse(it.day) to it.stars.toLong() }
    .zipWithNext { older, newer -> newer.first to (newer.second - older.second) }
    .groupBy({ (day, _) -> day.with(DayOfWeek.SUNDAY) }, { (_, gain) -> gain })
    .map { (weekStart, gains) -> weekStart.toString() to gains.sum() }
    .sortedBy { (weekStart, _) -> weekStart }

/**
 * Hand-drawn polyline anchored at zero so losses dip below the baseline;
 * min/max labels in a left gutter show the value range. Dragging a finger
 * across the chart scrubs it: the nearest point gets a dot, and the gesture
 * reports that point's center so the caller can anchor a tooltip above the
 * canvas. No charting dependency ships with the app.
 */
@Composable
private fun HistorySparkline(
    values: List<Long>,
    contentDescription: String,
    selectedIndex: Int?,
    onScrub: (index: Int?, pointX: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = lineColor.copy(alpha = 0.20f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    // The axis labels are the only scale reference the chart has, so both
    // extremes are always drawn (0 and the max for zero-based series).
    val maxLabel = CommonUtil.formatCount(maxOf(0L, values.max()))
    val minLabel = CommonUtil.formatCount(minOf(0L, values.min()))
    // Measured once in composition so the gesture handler and the draw
    // pass share the exact same gutter and plot geometry.
    val maxLayout = remember(textMeasurer, maxLabel, labelStyle) {
        textMeasurer.measure(maxLabel, labelStyle)
    }
    val minLayout = remember(textMeasurer, minLabel, labelStyle) {
        textMeasurer.measure(minLabel, labelStyle)
    }

    Canvas(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(values) {
                // Mirrors the draw-side geometry: any drift would place the
                // dot on the wrong point.
                val strokeWidth = 2.dp.toPx()
                val inset = strokeWidth / 2f
                val gutter = maxOf(maxLayout.size.width, minLayout.size.width) + 8.dp.toPx()
                val plotWidth = size.width - inset * 2 - gutter
                val plotLeft = inset + gutter
                fun indexAt(x: Float): Int {
                    if (values.size <= 1) return 0
                    val fraction = ((x - plotLeft) / plotWidth).coerceIn(0f, 1f)
                    return (fraction * (values.size - 1)).roundToInt()
                }

                // The tooltip must sit over the dot, not the raw touch, so
                // the reported x is the selected point's center.
                fun xFor(index: Int): Float = if (values.size <= 1) {
                    plotLeft + plotWidth / 2f
                } else {
                    plotLeft + plotWidth * index / (values.size - 1)
                }

                fun report(position: Offset) {
                    val index = indexAt(position.x)
                    onScrub(index, xFor(index))
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    report(down.position)
                    // Pick an axis at the touch slop: a drag that turns
                    // vertical is a page scroll, so it must stop driving the
                    // scrub (a per-move state write would recompose this node
                    // every frame mid-scroll). Horizontal drags consume the
                    // changes so the list above does not twitch underneath.
                    var decided = false
                    var horizontal = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            onScrub(null, 0f)
                            break
                        }
                        if (!decided) {
                            val dx = abs(change.position.x - down.position.x)
                            val dy = abs(change.position.y - down.position.y)
                            val slop = viewConfiguration.touchSlop
                            if (dx > slop || dy > slop) {
                                decided = true
                                horizontal = dx > dy
                                if (horizontal) {
                                    report(change.position)
                                    change.consume()
                                } else {
                                    // Scroll intent: drop the press-time
                                    // point and leave the rest to the list.
                                    onScrub(null, 0f)
                                    break
                                }
                            }
                        } else {
                            report(change.position)
                            change.consume()
                        }
                    }
                }
            }
    ) {
        val strokeWidth = 2.dp.toPx()
        // Half a stroke of breathing room so round caps are not clipped.
        val inset = strokeWidth / 2f
        val gutter = maxOf(maxLayout.size.width, minLayout.size.width) + 8.dp.toPx()
        val plotWidth = size.width - inset * 2 - gutter
        val plotHeight = size.height - inset * 2
        val plotLeft = inset + gutter

        val maxV = maxOf(0L, values.max())
        val minV = minOf(0L, values.min())
        val span = (maxV - minV).coerceAtLeast(1L).toFloat()

        fun yFor(value: Long): Float {
            val fraction = (value - minV) / span
            return inset + plotHeight * (1f - fraction)
        }

        fun pointAt(index: Int): Offset {
            val x = if (values.size == 1) {
                plotLeft + plotWidth / 2f
            } else {
                plotLeft + plotWidth * index / (values.size - 1)
            }
            return Offset(x, yFor(values[index]))
        }

        drawText(maxLayout, topLeft = Offset(inset, inset))
        drawText(
            minLayout,
            topLeft = Offset(inset, size.height - inset - minLayout.size.height)
        )

        val line = Path()
        values.indices.forEach { index ->
            val point = pointAt(index)
            if (index == 0) line.moveTo(point.x, point.y) else line.lineTo(point.x, point.y)
        }
        if (values.size == 1) {
            drawCircle(color = lineColor, radius = strokeWidth * 2f, center = pointAt(0))
        } else {
            drawPath(
                path = line,
                color = lineColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            // A light area down to the zero line keeps low-contrast series readable.
            val baselineY = yFor(0L)
            val area = Path().apply {
                addPath(line)
                lineTo(pointAt(values.size - 1).x, baselineY)
                lineTo(pointAt(0).x, baselineY)
                close()
            }
            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(fillColor, fillColor.copy(alpha = 0f))
                )
            )
        }

        val selected = selectedIndex?.coerceIn(0, values.lastIndex)
        if (selected != null) {
            val point = pointAt(selected)
            drawCircle(color = surfaceColor, radius = strokeWidth * 3f, center = point)
            drawCircle(color = lineColor, radius = strokeWidth * 2f, center = point)
        }
    }
}

/**
 * Popover tooltip for the scrubbed point: prebuilt multiline text (localized
 * date, value with bucket-size unit, cumulative total) drawn in its own
 * canvas sized to the text. It always parks fully above the chart with an
 * opaque background: a finger on the line reaches downward from its tip, so
 * anything below the touch ends up under the hand, and the rows above draw
 * before this node so the popover covers them instead of shining through.
 * Stays clamped to the chart's width.
 */
@Composable
private fun ChartTooltip(text: String, anchorX: Float, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall
        .copy(color = MaterialTheme.colorScheme.onSurface)
    // Elevated surface per the Material 3 tooltip treatment.
    val background = MaterialTheme.colorScheme.surfaceContainerHigh
    val layout = remember(textMeasurer, text, style) { textMeasurer.measure(text, style) }
    val density = LocalDensity.current
    val paddingPx = with(density) { 6.dp.toPx() }
    val gapPx = with(density) { 16.dp.toPx() }
    val cornerPx = with(density) { 12.dp.toPx() }
    val tipWidthPx = layout.size.width + paddingPx * 2
    val tipHeightPx = layout.size.height + paddingPx * 2

    Canvas(
        modifier = modifier
            .layout { measurable, constraints ->
                // The node spans the chart's width (fillMaxWidth below), so
                // the constraints clamp the popover to the chart's span. The
                // placement goes negative to float above the chart, which
                // Box does not clip.
                val placeable = measurable.measure(constraints)
                val maxWidth = constraints.maxWidth
                    .takeIf { it != Int.MAX_VALUE } ?: placeable.width
                val left = (anchorX - tipWidthPx / 2f)
                    .coerceIn(0f, maxOf(0f, maxWidth - tipWidthPx))
                layout(maxWidth, placeable.height) {
                    placeable.place(left.roundToInt(), -(tipHeightPx + gapPx).roundToInt())
                }
            }
            .fillMaxWidth()
            .height(with(density) { tipHeightPx.toDp() })
    ) {
        drawRoundRect(
            color = background,
            topLeft = Offset.Zero,
            size = Size(tipWidthPx, tipHeightPx),
            cornerRadius = CornerRadius(cornerPx)
        )
        drawText(layout, topLeft = Offset(paddingPx, paddingPx))
    }
}
