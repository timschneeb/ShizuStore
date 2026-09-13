/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

enum class DownloadFailure {
    DOWNLOAD_NETWORK,
    DOWNLOAD_HTTP,
    DOWNLOAD_HASH_MISMATCH,
    DOWNLOAD_ARCHIVE,
    DOWNLOAD_INSUFFICIENT_STORAGE,
    DOWNLOAD_MIRRORS_EXHAUSTED,
    INSTALL_ABORTED,
    INSTALL_BLOCKED,
    INSTALL_CONFLICT,
    INSTALL_INCOMPATIBLE,
    INSTALL_INVALID,
    INSTALL_STORAGE_FULL,
    INSTALL_SESSION_FAILURE,
    INSTALL_ROOT_UNAVAILABLE,
    INSTALL_SHIZUKU_UNAVAILABLE,
    INSTALL_APK_MISSING,
    INSTALL_SIGNER_MISMATCH,
    INSTALL_INTERRUPTED;

    val canRetryWithoutDownloading: Boolean
        get() = when (this) {
            DOWNLOAD_NETWORK,
            DOWNLOAD_HTTP,
            DOWNLOAD_HASH_MISMATCH,
            DOWNLOAD_ARCHIVE,
            DOWNLOAD_INSUFFICIENT_STORAGE,
            DOWNLOAD_MIRRORS_EXHAUSTED,
            INSTALL_APK_MISSING,
            INSTALL_INVALID -> false

            INSTALL_ABORTED,
            INSTALL_BLOCKED,
            INSTALL_CONFLICT,
            INSTALL_INCOMPATIBLE,
            INSTALL_STORAGE_FULL,
            INSTALL_SESSION_FAILURE,
            INSTALL_ROOT_UNAVAILABLE,
            INSTALL_SHIZUKU_UNAVAILABLE,
            INSTALL_SIGNER_MISMATCH,
            INSTALL_INTERRUPTED -> true
        }

    companion object {
        fun of(error: DownloadError): DownloadFailure = when (error) {
            is DownloadError.Network -> DOWNLOAD_NETWORK
            is DownloadError.Http -> DOWNLOAD_HTTP
            is DownloadError.HashMismatch -> DOWNLOAD_HASH_MISMATCH
            is DownloadError.Archive -> DOWNLOAD_ARCHIVE
            is DownloadError.InsufficientStorage -> DOWNLOAD_INSUFFICIENT_STORAGE
            is DownloadError.MirrorExhausted -> DOWNLOAD_MIRRORS_EXHAUSTED
        }

        fun of(error: InstallError): DownloadFailure = when (error) {
            is InstallError.Aborted -> INSTALL_ABORTED
            is InstallError.Blocked -> INSTALL_BLOCKED
            is InstallError.Conflict -> INSTALL_CONFLICT
            is InstallError.Incompatible -> INSTALL_INCOMPATIBLE
            is InstallError.Invalid -> INSTALL_INVALID
            is InstallError.StorageFull -> INSTALL_STORAGE_FULL
            is InstallError.SessionFailure -> INSTALL_SESSION_FAILURE
            InstallError.RootUnavailable -> INSTALL_ROOT_UNAVAILABLE
            InstallError.ShizukuUnavailable -> INSTALL_SHIZUKU_UNAVAILABLE
            is InstallError.ApkMissing -> INSTALL_APK_MISSING
            is InstallError.SignerMismatch -> INSTALL_SIGNER_MISMATCH
        }
    }
}
