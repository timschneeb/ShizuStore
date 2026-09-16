/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Droid-ify (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.network

@JvmInline
value class DataSize(val value: Long)

infix fun DataSize.percentBy(denominator: DataSize?): Int = value percentBy denominator?.value

infix fun Long.percentBy(denominator: Long?): Int {
    if (denominator == null || denominator < 1) return -1
    return (this * 100 / denominator).toInt()
}
