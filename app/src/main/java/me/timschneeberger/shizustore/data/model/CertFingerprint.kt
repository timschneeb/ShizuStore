/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import me.timschneeberger.shizustore.util.Hashing

/**
 * Signing identity fingerprints in lowercase hex. Server `sigSha256`/`sigMd5` columns are
 * space-joined sets (key rotation), so both the installed and the candidate side are sets
 * and matching is set intersection, never whole-string equality.
 */
data class CertFingerprint(
    val sha256: Set<String>,
    val md5: Set<String>
) {
    val isKnown: Boolean get() = sha256.isNotEmpty() || md5.isNotEmpty()

    companion object {

        fun of(sha256: String?, md5: String?): CertFingerprint = CertFingerprint(
            sha256 = parseFingerprintSet(sha256),
            md5 = parseFingerprintSet(md5)
        )

        fun fromEncodedCertificate(encoded: ByteArray): CertFingerprint = CertFingerprint(
            sha256 = setOf(Hashing.sha256Hex(encoded)),
            md5 = setOf(Hashing.md5Hex(encoded))
        )
    }
}

/** Parses a space-separated fingerprint set, normalizing case and ignoring blanks. */
fun parseFingerprintSet(raw: String?): Set<String> = raw
    .orEmpty()
    .split(' ', '\t', '\n', '\r')
    .map { it.trim().lowercase() }
    .filter { it.isNotEmpty() }
    .toSet()

/** True when any value in the space-joined [raw] set is present in [values]. */
fun fingerprintSetIntersects(raw: String?, values: Set<String>): Boolean {
    if (raw.isNullOrBlank() || values.isEmpty()) return false
    return parseFingerprintSet(raw).any { it in values }
}

/** True when [installed] matches the candidate signing sets, by SHA-256 or MD5 membership. */
fun fingerprintMatches(installed: CertFingerprint, sigSha256: String?, sigMd5: String?): Boolean {
    if (!installed.isKnown) return false
    return fingerprintSetIntersects(sigSha256, installed.sha256) ||
        fingerprintSetIntersects(sigMd5, installed.md5)
}

/** True when any of the installed signing identities matches the candidate signing sets. */
fun signaturesMatch(
    installed: List<CertFingerprint>,
    sigSha256: String?,
    sigMd5: String?
): Boolean = installed.any { fingerprintMatches(it, sigSha256, sigMd5) }
