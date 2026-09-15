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
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.cert.X509Certificate
import me.timschneeberger.shizustore.data.model.Algorithm
import me.timschneeberger.shizustore.data.model.CertFingerprint
import me.timschneeberger.shizustore.extensions.TAG
import me.timschneeberger.shizustore.extensions.generateX509Certificate
import me.timschneeberger.shizustore.extensions.getUpdateOwnerPackageNameCompat
import me.timschneeberger.shizustore.extensions.isPAndAbove
import me.timschneeberger.shizustore.extensions.isTAndAbove

object CertUtil {

    const val GOOGLE_ACCOUNT_TYPE = "com.google"
    const val GOOGLE_PLAY_AUTH_TOKEN_TYPE = "oauth2:https://www.googleapis.com/auth/googleplay"
    const val GOOGLE_PLAY_CERT =
        "MIIEQzCCAyugAwIBAgIJAMLgh0ZkSjCNMA0GCSqGSIb3DQEBBAUAMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDAeFw0wODA4MjEyMzEzMzRaFw0zNjAxMDcyMzEzMzRaMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBAKtWLgDYO6IIrgqWbxJOKdoR8qtW0I9Y4sypEwPpt1TTcvZApxsdyxMJZ2JORland2qSGT2y5b+3JKkedxiLDmpHpDsz2WCbdxgxRczfey5YZnTJ4VZbH0xqWVW/8lGmPav5xVwnIiJS6HXk+BVKZF+JcWjAsb/GEuq/eFdpuzSqeYTcfi6idkyugwfYwXFU1+5fZKUaRKYCwkkFQVfcAs1fXA5V+++FGfvjJ/CxURaSxaBvGdGDhfXE28LWuT9ozCl5xw4Yq5OGazvV24mZVSoOO0yZ31j7kYvtwYK6NeADwbSxDdJEqO4k//0zOHKrUiGYXtqw/A0LFFtqoZKFjnkCAQOjgdkwgdYwHQYDVR0OBBYEFMd9jMIhF1Ylmn/Tgt9r45jk14alMIGmBgNVHSMEgZ4wgZuAFMd9jMIhF1Ylmn/Tgt9r45jk14aloXikdjB0MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEUMBIGA1UEChMLR29vZ2xlIEluYy4xEDAOBgNVBAsTB0FuZHJvaWQxEDAOBgNVBAMTB0FuZHJvaWSCCQDC4IdGZEowjTAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBAUAA4IBAQBt0lLO74UwLDYKqs6Tm8/yzKkEu116FmH4rkaymUIE0P9KaMftGlMexFlaYjzmB2OxZyl6euNXEsQH8gjwyxCUKRJNexBiGcCEyj6z+a1fuHHvkiaai+KL8W1EyNmgjmyy8AW7P+LLlkR+ho5zEHatRbM/YAnqGcFh5iZBqpknHf1SKMXFh4dd239FJ1jWYfbMDMy3NS5CTMQ2XFI1MvcyUTdZPErjQfTbQe3aDQsQcafEQPD+nqActifKZ0Np0IS9L9kR/wbNvyz6ENwPiTrjV2KRkEjH78ZMcUQXg0L3BYHJ3lc69Vs5Ddf9uUGGMYldX3WfMBEmh/9iFBDAaTCK"

    private val fdroidPackages = listOf(
        "org.fdroid.basic",
        "org.fdroid.fdroid",
        "org.fdroid.fdroid.privileged",
        "com.looker.droidify",
        "com.machiav3lli.fdroid"
    )

    fun isFDroidApp(context: Context, packageName: String): Boolean =
        isInstalledByFDroid(context, packageName) || isSignedByFDroid(context, packageName)

    fun getEncodedCertificateHashes(context: Context, packageName: String): List<String> = try {
        val certificates = getX509Certificates(context, packageName)
        certificates.map {
            val messageDigest = MessageDigest.getInstance(Algorithm.SHA.value)
            messageDigest.update(it.encoded)
            Base64.encodeToString(
                messageDigest.digest(),
                Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
            )
        }
    } catch (exception: Exception) {
        Log.e(TAG, "Failed to get SHA256 certificate hash", exception)
        emptyList()
    }

    /** Lowercase hex SHA-256, as in F-Droid's `SignerV2.sha256`; other forms compare unequal. */
    fun getSigningCertificateSha256(context: Context, packageName: String): String? =
        getCurrentSigningCertificate(context, packageName)?.let { Hashing.sha256Hex(it.encoded) }

    fun getSigningCertificateMd5(context: Context, packageName: String): String? =
        getCurrentSigningCertificate(context, packageName)?.let {
            Hashing.md5Hex(it.encoded)
        }

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

    /** `signingCertificateHistory` is rotation-ordered: the current certificate is the last. */
    private fun getCurrentSigningCertificate(
        context: Context,
        packageName: String
    ): X509Certificate? = try {
        val packageInfo = getPackageInfoWithSignature(context, packageName)
        if (isPAndAbove) {
            val signingInfo = packageInfo.signingInfo!!
            if (signingInfo.hasMultipleSigners()) {
                null
            } else {
                signingInfo.signingCertificateHistory?.lastOrNull()?.generateX509Certificate()
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.singleOrNull()?.generateX509Certificate()
        }
    } catch (exception: Exception) {
        Log.e(TAG, "Failed to get the current signing certificate for $packageName", exception)
        null
    }

    private fun isSignedByFDroid(context: Context, packageName: String): Boolean = try {
        getX509Certificates(context, packageName).any { cert ->
            cert.subjectDN.name
                .split(",")
                .mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toMap()["O"] == "fdroid.org"
        }
    } catch (exception: Exception) {
        Log.e(TAG, "Failed to check signing cert for $packageName", exception)
        false
    }

    private fun isInstalledByFDroid(context: Context, packageName: String): Boolean =
        fdroidPackages.contains(
            context.packageManager.getUpdateOwnerPackageNameCompat(packageName)
        )

    internal fun getX509Certificates(context: Context, packageName: String): List<X509Certificate> =
        try {
            val packageInfo = getPackageInfoWithSignature(context, packageName)
            if (isPAndAbove) {
                if (packageInfo.signingInfo!!.hasMultipleSigners()) {
                    packageInfo.signingInfo!!.apkContentsSigners.map {
                        it.generateX509Certificate()
                    }
                } else {
                    packageInfo.signingInfo!!.signingCertificateHistory.map {
                        it.generateX509Certificate()
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures!!.map { it.generateX509Certificate() }
            }
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to get X509 certificates", exception)
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
