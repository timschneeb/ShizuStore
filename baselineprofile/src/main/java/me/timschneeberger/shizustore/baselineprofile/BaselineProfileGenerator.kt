/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.baselineprofile

import android.os.SystemClock
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
            startAtHome()
            // The first iteration syncs the catalog; openSearchResult blocks
            // until the result row exists, so later iterations also capture the
            // populated list and detail composition paths.
            SystemClock.sleep(1_500)
            swipeUp(0.72f, 0.28f)
            device.waitForIdle()

            clickText("Updates")
            device.waitForIdle()
            clickText("Search")
            device.waitForIdle()

            openSearchResult(query = "Obtain", result = "Obtainium")
            clickText("Obtainium")
            device.waitForIdle()

            SystemClock.sleep(1_500)
            swipeUp(0.72f, 0.28f)
            device.waitForIdle()
        }
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.swipeUp(from: Float, to: Float) {
        device.swipe(
            device.displayWidth / 2,
            (device.displayHeight * from).toInt(),
            device.displayWidth / 2,
            (device.displayHeight * to).toInt(),
            10
        )
    }
}
