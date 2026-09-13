/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CommonUtilTest {

    @Test
    fun formatCountKeepsSmallValues() {
        assertEquals("0", CommonUtil.formatCount(0L))
        assertEquals("999", CommonUtil.formatCount(999L))
    }

    @Test
    fun formatCountScalesToThousands() {
        assertEquals("1k", CommonUtil.formatCount(1_000L))
        assertEquals("10.1k", CommonUtil.formatCount(10_137L))
    }

    @Test
    fun formatCountScalesToMillionsAndBillions() {
        assertEquals("1M", CommonUtil.formatCount(1_000_000L))
        assertEquals("2.5B", CommonUtil.formatCount(2_500_000_000L))
    }
}
