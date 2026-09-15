/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Serializes API calls with a minimum spacing and exponential backoff for 429s.
 * The server is a fixed-window 100 req/min per IP with no queue, so a client
 * that bursts gets rejected; spacing stays under the window and 429s back off
 * further.
 *
 * Clock and sleep are injectable so tests never touch wall time.
 */
class RequestThrottle(
    private val minSpacingMillis: Long = DEFAULT_MIN_SPACING_MILLIS,
    private val initialBackoffMillis: Long = DEFAULT_INITIAL_BACKOFF_MILLIS,
    private val maxBackoffMillis: Long = DEFAULT_MAX_BACKOFF_MILLIS,
    private val now: () -> Long = { System.nanoTime() / NANOS_PER_MILLI },
    private val sleep: suspend (Long) -> Unit = { delay(it) }
) {
    private val mutex = Mutex()
    private var nextSlotAt = 0L
    private var blockedUntil = 0L
    private var backoffMillis = 0L

    /** Waits for the next allowed slot, then reserves it. */
    suspend fun acquire() {
        mutex.withLock {
            val current = now()
            val wait = maxOf(nextSlotAt - current, blockedUntil - current, 0L)
            if (wait > 0L) sleep(wait)
            nextSlotAt = now() + minSpacingMillis
        }
    }

    suspend fun penalize() {
        mutex.withLock {
            backoffMillis =
                if (backoffMillis == 0L) {
                    initialBackoffMillis
                } else {
                    (backoffMillis * 2).coerceAtMost(maxBackoffMillis)
                }
            blockedUntil = now() + backoffMillis
        }
    }

    suspend fun reset() {
        mutex.withLock {
            backoffMillis = 0L
            blockedUntil = 0L
        }
    }

    companion object {
        const val DEFAULT_MIN_SPACING_MILLIS = 650L
        const val DEFAULT_INITIAL_BACKOFF_MILLIS = 5_000L
        const val DEFAULT_MAX_BACKOFF_MILLIS = 60_000L

        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
