/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

/**
 * Maps Android API levels to the marketing version users recognize (API 34 -> "14").
 * Null when this build has no name for the level, so callers can fall back to the number.
 */
object AndroidVersion {
    private val names: Map<Int, String> = mapOf(
        1 to "1.0",
        2 to "1.1",
        3 to "1.5",
        4 to "1.6",
        5 to "2.0",
        6 to "2.0.1",
        7 to "2.1",
        8 to "2.2",
        9 to "2.3",
        10 to "2.3.3",
        11 to "3.0",
        12 to "3.1",
        13 to "3.2",
        14 to "4.0",
        15 to "4.0.3",
        16 to "4.1",
        17 to "4.2",
        18 to "4.3",
        19 to "4.4",
        20 to "4.4W",
        21 to "5.0",
        22 to "5.1",
        23 to "6.0",
        24 to "7.0",
        25 to "7.1",
        26 to "8.0",
        27 to "8.1",
        28 to "9",
        29 to "10",
        30 to "11",
        31 to "12",
        32 to "12L",
        33 to "13",
        34 to "14",
        35 to "15",
        36 to "16",
        37 to "17"
    )

    fun name(apiLevel: Int): String? = names[apiLevel]
}
