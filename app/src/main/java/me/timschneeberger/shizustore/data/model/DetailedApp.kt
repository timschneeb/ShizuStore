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
    val packageName: String? get() = app.packageName

    /** Candidate to offer for a fresh install: primary first, then highest version. */
    val primaryCandidate: AppCandidate? get() = candidates.firstOrNull()

    fun candidateFor(installed: CertFingerprint?): AppCandidate? =
        candidates.firstOrNull { it.matchesInstalled(installed) }
}
