/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import me.timschneeberger.shizustore.data.room.entity.AppEntity

data class DetailedApp(
    val app: AppEntity,
    val candidates: List<AppCandidate>
) {
    /** Candidate to offer for a fresh install: first one runnable on this device (server-primary first). */
    val primaryCandidate: AppCandidate?
        get() = candidates.firstOrNull { it.supportsAbi(AppCandidate.deviceAbis) }
            ?: candidates.firstOrNull()
}
