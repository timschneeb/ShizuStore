/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose

import androidx.annotation.StringRes
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.model.DownloadError
import me.timschneeberger.shizustore.data.model.DownloadFailure
import me.timschneeberger.shizustore.data.model.InstallError
import me.timschneeberger.shizustore.viewmodel.UnattendedPausedReason

@StringRes
fun InstallError.stringRes(): Int = when (this) {
    is InstallError.Aborted -> R.string.error_install_aborted
    is InstallError.Blocked -> R.string.error_install_blocked
    is InstallError.Conflict -> R.string.error_install_conflict
    is InstallError.Incompatible -> R.string.error_install_incompatible
    is InstallError.Invalid -> R.string.error_install_invalid
    is InstallError.StorageFull -> R.string.error_install_storage_full
    is InstallError.SessionFailure -> R.string.error_install_session_failure
    InstallError.RootUnavailable -> R.string.error_install_root_unavailable
    InstallError.ShizukuUnavailable -> R.string.error_install_shizuku_unavailable
    is InstallError.ApkMissing -> R.string.error_install_apk_missing
    is InstallError.SignerMismatch -> R.string.error_install_signer_mismatch
}

@StringRes
fun DownloadError.stringRes(): Int = when (this) {
    is DownloadError.Network -> R.string.error_download_network
    is DownloadError.Http -> R.string.error_download_http
    is DownloadError.HashMismatch -> R.string.error_hash_mismatch
    is DownloadError.Archive -> R.string.error_download_archive
    is DownloadError.InsufficientStorage -> R.string.error_download_insufficient_storage
    is DownloadError.MirrorExhausted -> R.string.error_download_mirrors_exhausted
}

@StringRes
fun DownloadFailure.stringRes(): Int = when (this) {
    DownloadFailure.DOWNLOAD_NETWORK -> R.string.error_download_network
    DownloadFailure.DOWNLOAD_HTTP -> R.string.error_download_http
    DownloadFailure.DOWNLOAD_HASH_MISMATCH -> R.string.error_hash_mismatch
    DownloadFailure.DOWNLOAD_ARCHIVE -> R.string.error_download_archive
    DownloadFailure.DOWNLOAD_INSUFFICIENT_STORAGE ->
        R.string.error_download_insufficient_storage

    DownloadFailure.DOWNLOAD_MIRRORS_EXHAUSTED -> R.string.error_download_mirrors_exhausted
    DownloadFailure.INSTALL_ABORTED -> R.string.error_install_aborted
    DownloadFailure.INSTALL_BLOCKED -> R.string.error_install_blocked
    DownloadFailure.INSTALL_CONFLICT -> R.string.error_install_conflict
    DownloadFailure.INSTALL_INCOMPATIBLE -> R.string.error_install_incompatible
    DownloadFailure.INSTALL_INVALID -> R.string.error_install_invalid
    DownloadFailure.INSTALL_STORAGE_FULL -> R.string.error_install_storage_full
    DownloadFailure.INSTALL_SESSION_FAILURE -> R.string.error_install_session_failure
    DownloadFailure.INSTALL_ROOT_UNAVAILABLE -> R.string.error_install_root_unavailable
    DownloadFailure.INSTALL_SHIZUKU_UNAVAILABLE -> R.string.error_install_shizuku_unavailable
    DownloadFailure.INSTALL_APK_MISSING -> R.string.error_install_apk_missing
    DownloadFailure.INSTALL_SIGNER_MISMATCH -> R.string.error_install_signer_mismatch
    DownloadFailure.INSTALL_INTERRUPTED -> R.string.error_install_interrupted
}

@StringRes
fun UnattendedPausedReason.stringRes(): Int = when (this) {
    UnattendedPausedReason.UPDATE_CHECKS_OFF -> R.string.settings_unattended_paused_checks_off

    UnattendedPausedReason.INSTALLER_NEEDS_CONFIRMATION ->
        R.string.settings_unattended_paused_confirmation

    UnattendedPausedReason.SHIZUKU_NOT_READY -> R.string.settings_unattended_paused_shizuku
    UnattendedPausedReason.ROOT_UNAVAILABLE -> R.string.settings_unattended_paused_root
}
