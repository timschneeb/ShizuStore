/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestThrottleTest {
    private class FakeClock {
        var nowMillis = 0L
        val sleeps = mutableListOf<Long>()

        fun sleep(millis: Long) {
            sleeps += millis
            nowMillis += millis
        }
    }

    private fun throttle(clock: FakeClock, maxBackoff: Long = 2_000L) = RequestThrottle(
        minSpacingMillis = 100L,
        initialBackoffMillis = 500L,
        maxBackoffMillis = maxBackoff,
        now = { clock.nowMillis },
        sleep = clock::sleep
    )

    @Test
    fun spacesConsecutiveCalls() = runTest {
        val clock = FakeClock()
        val throttle = throttle(clock)

        throttle.acquire()
        throttle.acquire()

        assertEquals(listOf(100L), clock.sleeps)
    }

    @Test
    fun backsOffExponentiallyAfterRateLimit() = runTest {
        val clock = FakeClock()
        val throttle = throttle(clock)

        throttle.acquire()
        throttle.acquire()

        throttle.penalize()
        throttle.acquire()

        throttle.penalize()
        throttle.acquire()

        assertEquals(listOf(100L, 500L, 1_000L), clock.sleeps)
    }

    @Test
    fun capsBackoffAndResetsOnSuccess() = runTest {
        val clock = FakeClock()
        val throttle = throttle(clock, maxBackoff = 1_200L)

        repeat(4) { throttle.penalize() }
        throttle.acquire()
        assertTrue(clock.sleeps.single() <= 1_200L)

        throttle.reset()
        clock.sleeps.clear()
        throttle.acquire()
        clock.sleeps.clear()
        throttle.acquire()
        assertEquals(listOf(100L), clock.sleeps)
    }
}
