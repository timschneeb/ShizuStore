/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import android.content.pm.PackageInstaller

sealed interface InstallError {
    data class Aborted(val packageName: String) : InstallError
    data class Blocked(val packageName: String, val blockedBy: String?) : InstallError
    data class Conflict(val packageName: String) : InstallError
    data class Incompatible(val packageName: String) : InstallError
    data class Invalid(val packageName: String) : InstallError
    data class StorageFull(val packageName: String) : InstallError
    data class SessionFailure(val packageName: String, val reason: String?) : InstallError
    data object RootUnavailable : InstallError
    data object ShizukuUnavailable : InstallError
    data class ApkMissing(val packageName: String) : InstallError
    data class SignerMismatch(val packageName: String) : InstallError

    companion object {
        fun fromSessionStatus(status: Int, packageName: String, message: String?): InstallError =
            when (status) {
                PackageInstaller.STATUS_FAILURE_ABORTED -> Aborted(packageName)
                PackageInstaller.STATUS_FAILURE_BLOCKED -> Blocked(packageName, message)
                PackageInstaller.STATUS_FAILURE_CONFLICT -> Conflict(packageName)
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> Incompatible(packageName)
                PackageInstaller.STATUS_FAILURE_INVALID -> Invalid(packageName)
                PackageInstaller.STATUS_FAILURE_STORAGE -> StorageFull(packageName)
                else -> SessionFailure(packageName, message)
            }
    }
}
