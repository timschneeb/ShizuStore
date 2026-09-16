/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PackageInfoFlags
import android.util.Log
import java.security.cert.X509Certificate
import me.timschneeberger.shizustore.data.model.CertFingerprint
import me.timschneeberger.shizustore.extensions.TAG
import me.timschneeberger.shizustore.extensions.generateX509Certificate
import me.timschneeberger.shizustore.extensions.isPAndAbove
import me.timschneeberger.shizustore.extensions.isTAndAbove

object CertUtil {

    /**
     * Every current signing certificate as its own fingerprint, so callers can match
     * a candidate's signing set.
     */
    fun getSigningFingerprints(context: Context, packageName: String): List<CertFingerprint> =
        getCurrentSigningCertificates(context, packageName).map {
            CertFingerprint.fromEncodedCertificate(it.encoded)
        }

    /** Current certificate only: `signingCertificateHistory` ends at the active cert. */
    private fun getCurrentSigningCertificates(
        context: Context,
        packageName: String
    ): List<X509Certificate> = try {
        val packageInfo = getPackageInfoWithSignature(context, packageName)
        if (isPAndAbove) {
            val signingInfo = packageInfo.signingInfo!!
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners.map { it.generateX509Certificate() }
            } else {
                listOfNotNull(
                    signingInfo.signingCertificateHistory?.lastOrNull()?.generateX509Certificate()
                )
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.map { it.generateX509Certificate() }.orEmpty()
        }
    } catch (exception: Exception) {
        Log.e(TAG, "Failed to get current signing certificates for $packageName", exception)
        emptyList()
    }

    private fun getPackageInfoWithSignature(context: Context, packageName: String): PackageInfo =
        if (isPAndAbove) {
            getPackageInfo(context, packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            @Suppress("DEPRECATION")
            getPackageInfo(context, packageName, PackageManager.GET_SIGNATURES)
        }

    @Throws(Exception::class)
    private fun getPackageInfo(context: Context, packageName: String, flags: Int = 0): PackageInfo =
        if (isTAndAbove) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageInfoFlags.of(flags.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, flags)
        }
}
