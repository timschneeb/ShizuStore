/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import me.timschneeberger.shizustore.data.room.entity.AppEntity

/** Catalog app joined with its installable candidates, as shown on the details screen. */
data class DetailedApp(
    val app: AppEntity,
    val candidates: List<AppCandidate>
) {
    val packageName: String? get() = app.packageName

    /** Candidate to offer for a fresh install: primary first, then highest version. */
    val primaryCandidate: AppCandidate? get() = candidates.firstOrNull()

    /** Candidate matching the installed signing identity, if any. */
    fun candidateFor(installed: CertFingerprint?): AppCandidate? =
        candidates.firstOrNull { it.matchesInstalled(installed) }
}
