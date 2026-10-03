/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

internal const val TARGET_PACKAGE = "me.timschneeberger.shizustore"
internal const val UI_TIMEOUT_MS = 60_000L

// First activity start after a profile reset occasionally produces no gfxinfo
// frame on this device (Macrobenchmark fails with "Unable to confirm activity
// launch completion"); a throwaway start absorbs that. The screen is woken as
// well: a dozing display yields no frames at all, which looks like the same
// failure. The process is stopped again afterwards so callers stay compatible
// with the cold-start precondition.
internal fun MacrobenchmarkScope.warmUpLaunch() {
    device.wakeUp()
    device.executeShellCommand("am force-stop $TARGET_PACKAGE")
    pressHome()
    runCatching { startActivityAndWait() }
    device.executeShellCommand("am force-stop $TARGET_PACKAGE")
}

// Deterministic start state for frame benchmarks: fresh process on the Apps tab.
// The first run also waits out the initial catalog sync inside openSearchResult.
internal fun MacrobenchmarkScope.startAtHome() {
    warmUpLaunch()
    pressHome()
    startActivityAndWait()
    check(device.wait(Until.hasObject(By.text("Apps")), UI_TIMEOUT_MS)) {
        "Main screen did not appear"
    }
}

// Search screen with a query whose exact name is the only match.
internal fun MacrobenchmarkScope.openSearchResult(query: String, result: String) {
    clickText("Search")
    val field = device.wait(Until.findObject(By.clazz("android.widget.EditText")), UI_TIMEOUT_MS)
    checkNotNull(field) { "Search field did not appear" }
    field.click()
    device.executeShellCommand("input text $query")
    check(device.wait(Until.hasObject(By.text(result)), UI_TIMEOUT_MS)) {
        "No search result for $query"
    }
}

// A row can be recycled between the find and the click while the list settles;
// retry so a live list does not fail the whole iteration.
internal fun MacrobenchmarkScope.clickText(text: String) {
    repeat(5) {
        val node = device.wait(Until.findObject(By.text(text)), UI_TIMEOUT_MS)
        if (node != null && runCatching { node.click() }.isSuccess) return
        device.waitForIdle()
    }
    error("Could not click \"$text\"")
}

// The tallest scrollable is the vertical list; nested carousel strips and the
// filter chip row are shorter, so a coordinate-free fling cannot land on them.
// Handles go stale while the list swaps skeletons for rows, so bounds reads
// are best-effort and a stale candidate simply sorts as zero height.
private fun MacrobenchmarkScope.listScroller(): UiObject2? =
    device.findObjects(By.scrollable(true))
        .maxByOrNull { runCatching { it.visibleBounds.height() }.getOrDefault(0) }

// Skeleton placeholders carry no text, real rows do. Compose nests row text
// below the item node, so the probe searches descendants instead of direct
// children. Flinging only after rows appear keeps the frame stats a measure of
// the list, not of the loading state.
private fun MacrobenchmarkScope.awaitLoadedList() {
    val deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MS
    val textProbe = Pattern.compile("\\S+")
    while (SystemClock.uptimeMillis() < deadline) {
        val scroller = listScroller()
        val loaded = scroller != null && runCatching {
            scroller.findObjects(By.text(textProbe)).size >= 2
        }.getOrDefault(false)
        if (loaded) return
        device.waitForIdle(250)
    }
    error("No loaded scrollable list appeared")
}

// Repeated flings keep the metric on sustained scroll frames instead of the
// single touch-down beat.
private fun MacrobenchmarkScope.flingList() {
    awaitLoadedList()
    repeat(SCROLL_FLINGS) {
        check(flingOnce()) { "Could not fling the list" }
        device.waitForIdle()
    }
}

// Re-resolve the scroller on every attempt: a fling on a stale handle no-ops
// and the row swap that caused it has usually settled by the next try.
// The gesture is a raw swipe inside the list bounds with an explicit duration:
// UiObject2.fling picked the full-screen pager and short swipes read as clicks.
private fun MacrobenchmarkScope.flingOnce(): Boolean {
    repeat(5) {
        val scroller = listScroller() ?: return@repeat
        val bounds = runCatching { scroller.visibleBounds }.getOrNull() ?: return@repeat
        val x = bounds.centerX()
        val startY = bounds.top + (bounds.height() * 0.75f).toInt()
        val endY = bounds.top + (bounds.height() * 0.25f).toInt()
        val flung = runCatching {
            device.executeShellCommand("input swipe $x $startY $x $endY $FLING_DURATION_MS")
        }.isSuccess
        if (flung) return true
        device.waitForIdle(100)
    }
    return false
}

@RunWith(AndroidJUnit4::class)
class NavigationBenchmarks {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startup() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.None(),
        setupBlock = {
            warmUpLaunch()
        }
    ) {
        startActivityAndWait()
    }

    @Test
    fun switchTabs() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = {
            startAtHome()
        }
    ) {
        clickText("Updates")
        device.waitForIdle()
        clickText("Search")
        device.waitForIdle()
        clickText("Apps")
        device.waitForIdle()
    }

    @Test
    fun openAppDetails() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = {
            startAtHome()
            openSearchResult(query = "Obtain", result = "Obtainium")
        }
    ) {
        clickText("Obtainium")
        device.waitForIdle()
    }

    // Vertical flings through the curated home carousel, the heaviest lazy
    // composition (rows of tiles and two-row grids inside one list).
    @Test
    fun scrollHome() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = {
            startAtHome()
        }
    ) {
        flingList()
    }

    // Vertical flings through the full catalog list, reached through the search
    // home's "All" chip so the route never depends on catalog contents.
    @Test
    fun scrollAppList() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial(),
        setupBlock = {
            startAtHome()
            clickText("Search")
            clickText("All")
            check(device.wait(Until.gone(By.text("Categories")), UI_TIMEOUT_MS)) {
                "Search home did not give way to the app list"
            }
        }
    ) {
        flingList()
    }
}

private const val SCROLL_FLINGS = 3
private const val FLING_DURATION_MS = 150
