/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import android.content.Context
import java.text.DateFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.model.ProxyInfo

enum class AgeUnit { JUST_NOW, MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS }

data class AgeBucket(val unit: AgeUnit, val count: Long = 0L)

object CommonUtil {
    private val siPrefixes: Map<Int, String> = hashMapOf(
        Pair(0, ""),
        Pair(1, ""),
        Pair(3, " KB"),
        Pair(6, " MB"),
        Pair(9, " GB")
    )

    fun addSiPrefix(value: Long): String {
        if (value <= 1L) {
            return "NA"
        }
        var tempValue = value
        var order = 0
        while (tempValue >= 1000.0) {
            tempValue /= 1000.toLong()
            order += 3
        }
        return tempValue.toString() + siPrefixes[order]
    }

    /** Null when the size is unknown, so callers can hide the placeholder. */
    fun sizeLabel(value: Long): String? = if (value <= 1L) null else addSiPrefix(value)

    fun formatCount(value: Long): String {
        if (value < 1_000L) return value.toString()
        val units = listOf("k", "M", "B")
        var scaled = value.toDouble()
        var index = -1
        while (scaled >= 1_000.0 && index < units.lastIndex) {
            scaled /= 1_000.0
            index++
        }
        val text = String.format(Locale.getDefault(), "%.1f", scaled).removeSuffix(".0")
        return text + units[index]
    }

    fun getETAString(context: Context, etaInMilliSeconds: Long): String {
        if (etaInMilliSeconds <= 0) {
            return context.getString(R.string.download_eta_calculating)
        }
        var seconds = (etaInMilliSeconds / 1000).toInt()
        val hours = (seconds / 3600).toLong()
        seconds -= (hours * 3600).toInt()
        val minutes = (seconds / 60).toLong()
        seconds -= (minutes * 60).toInt()
        return when {
            hours > 0 -> {
                context.getString(R.string.download_eta_hrs, hours, minutes, seconds)
            }

            minutes > 0 -> {
                context.getString(R.string.download_eta_min, minutes, seconds)
            }

            else -> {
                context.getString(R.string.download_eta_sec, seconds)
            }
        }
    }

    fun humanReadableByteSpeed(bytes: Long, si: Boolean): String {
        val unit = if (si) 1000 else 1024
        if (bytes < unit) return "$bytes B/s"
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if (si) "" else "i"
        return String.format(
            Locale.getDefault(),
            "%.1f %sB/s",
            bytes / unit.toDouble().pow(exp.toDouble()),
            pre
        )
    }

    fun transferRateText(context: Context, speed: Long, timeRemaining: Long): String? {
        if (speed <= 0L) return null
        val rate = humanReadableByteSpeed(speed, true)
        if (timeRemaining <= 0L) return rate
        return "$rate · " + getETAString(context, timeRemaining)
    }

    fun parseProxyUrl(proxyUrl: String): ProxyInfo? {
        val pattern = """^(https?|socks5?)://(?:([^\s:@]+):([^\s:@]+)@)?([^\s:@]+):(\d+)$"""
        val match = pattern.toRegex().find(proxyUrl)

        return when {
            match != null -> {
                val protocol = match.groupValues[1].uppercase()
                val username = match.groupValues[2]
                val password = match.groupValues[3]
                val url = match.groupValues[4]
                val port = match.groupValues[5]

                ProxyInfo(
                    protocol,
                    url,
                    port.toInt(),
                    username,
                    password
                )
            }

            else -> null
        }
    }

    fun proxyUrl(info: ProxyInfo): String {
        val user = info.proxyUser
        val password = info.proxyPassword
        val credentials = if (!user.isNullOrBlank() && !password.isNullOrBlank()) {
            "$user:$password@"
        } else {
            ""
        }
        return "${info.protocol.lowercase(Locale.ROOT)}://$credentials${info.host}:${info.port}"
    }

    fun proxyDisplayText(info: ProxyInfo): String =
        "${info.protocol.lowercase(Locale.ROOT)}://${info.host}:${info.port}"

    /** Per-thread because DateFormat is not thread-safe; call sites are per-frame hot. */
    private val dateFormat: ThreadLocal<DateFormat> = object : ThreadLocal<DateFormat>() {
        override fun initialValue(): DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
    }

    fun formatDate(epochMillis: Long): String = dateFormat.get()!!.format(Date(epochMillis))

    /** Server timestamps are ISO-8601 strings (System.Text.Json DateTimeOffset). */
    fun parseIsoUtcMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun ageBucket(ageMillis: Long): AgeBucket = when {
        ageMillis < MINUTE_MILLIS -> AgeBucket(AgeUnit.JUST_NOW)
        ageMillis < HOUR_MILLIS -> AgeBucket(AgeUnit.MINUTES, ageMillis / MINUTE_MILLIS)
        ageMillis < DAY_MILLIS -> AgeBucket(AgeUnit.HOURS, ageMillis / HOUR_MILLIS)
        ageMillis < WEEK_MILLIS -> AgeBucket(AgeUnit.DAYS, ageMillis / DAY_MILLIS)
        ageMillis < MONTH_MILLIS -> AgeBucket(AgeUnit.WEEKS, ageMillis / WEEK_MILLIS)
        // A 30-day month runs out just short of a year; keep 360-364 days at 11
        // months instead of a one-off "12 months ago".
        ageMillis < YEAR_MILLIS -> AgeBucket(AgeUnit.MONTHS, (ageMillis / MONTH_MILLIS).coerceAtMost(11))
        else -> AgeBucket(AgeUnit.YEARS, ageMillis / YEAR_MILLIS)
    }

    /** Bucketed age for list rows, e.g. "3 days ago"; future stamps read as "just now". */
    fun relativeAge(context: Context, epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val age = ageBucket((now - epochMillis).coerceAtLeast(0L))
        val count = age.count.toInt()
        return when (age.unit) {
            AgeUnit.JUST_NOW -> context.getString(R.string.time_just_now)
            AgeUnit.MINUTES ->
                context.resources.getQuantityString(R.plurals.time_minutes_ago, count, count)
            AgeUnit.HOURS ->
                context.resources.getQuantityString(R.plurals.time_hours_ago, count, count)
            AgeUnit.DAYS ->
                context.resources.getQuantityString(R.plurals.time_days_ago, count, count)
            AgeUnit.WEEKS ->
                context.resources.getQuantityString(R.plurals.time_weeks_ago, count, count)
            AgeUnit.MONTHS ->
                context.resources.getQuantityString(R.plurals.time_months_ago, count, count)
            AgeUnit.YEARS ->
                context.resources.getQuantityString(R.plurals.time_years_ago, count, count)
        }
    }

    private const val MINUTE_MILLIS = 60_000L
    private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private const val DAY_MILLIS = 24 * HOUR_MILLIS
    private const val WEEK_MILLIS = 7 * DAY_MILLIS
    private const val MONTH_MILLIS = 30 * DAY_MILLIS
    private const val YEAR_MILLIS = 365 * DAY_MILLIS
}
