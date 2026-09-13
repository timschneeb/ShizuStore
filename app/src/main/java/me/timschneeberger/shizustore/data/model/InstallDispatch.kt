/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

sealed interface InstallDispatch {

    data object Started : InstallDispatch

    data class Refused(val status: DownloadStatus?) : InstallDispatch
}
