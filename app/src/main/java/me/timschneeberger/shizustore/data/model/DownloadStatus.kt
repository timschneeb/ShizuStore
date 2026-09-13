/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

enum class DownloadStatus {
    DOWNLOADING,
    FAILED,
    CANCELLED,
    COMPLETED,
    QUEUED,
    UNAVAILABLE,
    VERIFYING,
    INSTALLING,
    AWAITING_CONFIRMATION,
    INSTALLED;

    companion object {
        val finished = setOf(FAILED, CANCELLED, COMPLETED, INSTALLED)
        val running = setOf(QUEUED, DOWNLOADING)

        val processing = setOf(DOWNLOADING, VERIFYING)

        val installing = setOf(INSTALLING, AWAITING_CONFIRMATION)
    }
}
