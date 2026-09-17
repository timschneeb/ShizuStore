/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun parseIsoUtcMillisHandlesServerFormats() {
        val mayFirst = 1_777_593_600_000L
        assertEquals(mayFirst, CommonUtil.parseIsoUtcMillis("2026-05-01T00:00:00+00:00"))
        assertEquals(mayFirst, CommonUtil.parseIsoUtcMillis("2026-05-01T00:00:00Z"))
        assertEquals(mayFirst, CommonUtil.parseIsoUtcMillis("2026-05-01T02:00:00+02:00"))
        assertEquals(
            mayFirst + 123L,
            CommonUtil.parseIsoUtcMillis("2026-05-01T00:00:00.1234567+00:00")
        )
    }

    @Test
    fun parseIsoUtcMillisRejectsMissingAndInvalidValues() {
        assertNull(CommonUtil.parseIsoUtcMillis(null))
        assertNull(CommonUtil.parseIsoUtcMillis(""))
        assertNull(CommonUtil.parseIsoUtcMillis("  "))
        assertNull(CommonUtil.parseIsoUtcMillis("2026-05-01"))
    }

    @Test
    fun ageBucketClampsFutureStampsToJustNow() {
        assertEquals(AgeBucket(AgeUnit.JUST_NOW), CommonUtil.ageBucket(-1L))
        assertEquals(AgeBucket(AgeUnit.JUST_NOW), CommonUtil.ageBucket(0L))
        assertEquals(AgeBucket(AgeUnit.JUST_NOW), CommonUtil.ageBucket(59_999L))
    }

    @Test
    fun ageBucketPicksMinutesHoursAndDays() {
        assertEquals(AgeBucket(AgeUnit.MINUTES, 1L), CommonUtil.ageBucket(60_000L))
        assertEquals(AgeBucket(AgeUnit.MINUTES, 59L), CommonUtil.ageBucket(3_540_000L))
        assertEquals(AgeBucket(AgeUnit.HOURS, 1L), CommonUtil.ageBucket(3_600_000L))
        assertEquals(AgeBucket(AgeUnit.HOURS, 23L), CommonUtil.ageBucket(86_340_000L))
        assertEquals(AgeBucket(AgeUnit.DAYS, 1L), CommonUtil.ageBucket(86_400_000L))
        assertEquals(AgeBucket(AgeUnit.DAYS, 6L), CommonUtil.ageBucket(604_799_999L))
    }

    @Test
    fun ageBucketPicksWeeksMonthsAndYears() {
        val day = 86_400_000L
        assertEquals(AgeBucket(AgeUnit.WEEKS, 1L), CommonUtil.ageBucket(7 * day))
        assertEquals(AgeBucket(AgeUnit.WEEKS, 4L), CommonUtil.ageBucket(29 * day))
        assertEquals(AgeBucket(AgeUnit.MONTHS, 1L), CommonUtil.ageBucket(30 * day))
        assertEquals(AgeBucket(AgeUnit.MONTHS, 11L), CommonUtil.ageBucket(364 * day))
        assertEquals(AgeBucket(AgeUnit.YEARS, 1L), CommonUtil.ageBucket(365 * day))
        assertEquals(AgeBucket(AgeUnit.YEARS, 2L), CommonUtil.ageBucket(730 * day))
    }
}
