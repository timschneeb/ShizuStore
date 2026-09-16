/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import android.content.Context
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.model.ProxyInfo

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
}
