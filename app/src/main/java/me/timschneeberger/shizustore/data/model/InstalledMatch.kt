/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import me.timschneeberger.shizustore.data.room.entity.InstalledEntity

/**
 * Resolves the installed row that belongs to this listing. Two listings may share a
 * package, so a package match alone is not enough when the catalog knows signing keys:
 * a same-package fork signed with another key is not this entry.
 */
fun matchInstalled(
    canonicalPackage: String?,
    candidates: List<AppCandidate>,
    installedByPackage: Map<String, InstalledEntity>
): InstalledEntity? {
    val packages = buildSet {
        canonicalPackage?.let(::add)
        candidates.forEach { candidate -> candidate.packageName?.let(::add) }
    }
    val installedRows = packages.mapNotNull(installedByPackage::get)
    if (installedRows.isEmpty()) return null

    val signedCandidates = candidates.filter { it.hasSigningFingerprint }
    if (signedCandidates.isEmpty()) return installedRows.first()

    // The canonical package can be installed even when the catalog only ships
    // flavor candidates for it. Keep that row when its signing identity fits any
    // build of this listing; a same-package fork with another key stays separate.
    val canonicalInstalled = canonicalPackage?.let(installedByPackage::get)
    if (canonicalInstalled != null) {
        val fingerprint = CertFingerprint.of(
            canonicalInstalled.signer,
            canonicalInstalled.signerMd5
        )
        val canonicalCandidates = signedCandidates.filter {
            it.packageName == canonicalInstalled.packageName
        }
        val matches = when {
            canonicalCandidates.isNotEmpty() ->
                canonicalCandidates.any { it.matchesInstalled(fingerprint) }

            !fingerprint.isKnown -> true
            else -> signedCandidates.any { it.matchesInstalled(fingerprint) }
        }
        if (matches) return canonicalInstalled
    }

    return installedRows.firstOrNull { installed ->
        val fingerprint = CertFingerprint.of(installed.signer, installed.signerMd5)
        signedCandidates.any { candidate ->
            candidate.packageName == installed.packageName &&
                (!fingerprint.isKnown || candidate.matchesInstalled(fingerprint))
        }
    }
}
