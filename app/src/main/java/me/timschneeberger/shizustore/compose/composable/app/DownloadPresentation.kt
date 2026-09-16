/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * The downloads row treatment is Aurora Store's, read from its DownloadListItem.
 */

package me.timschneeberger.shizustore.compose.composable.app

import android.content.Context
import androidx.compose.runtime.Immutable
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.stringRes
import me.timschneeberger.shizustore.data.api.ShizuUrls
import me.timschneeberger.shizustore.data.model.DownloadFailure
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.util.CommonUtil
import me.timschneeberger.shizustore.util.ServerConfig

/**
 * The Room entity has public vars and a list, so it never skips as a composable
 * parameter: rows take this flattened snapshot instead and only the row that
 * actually changed recomposes on a progress tick.
 */
@Immutable
internal data class DownloadRowState(
    val packageName: String,
    val displayName: String,
    val iconUrl: String,
    val status: DownloadStatus,
    val progress: Int,
    val speed: Long,
    val timeRemaining: Long,
    val downloadedAt: Long,
    val error: DownloadFailure?,
    val isActive: Boolean,
    val isFinished: Boolean
)

internal fun Download.toRowState(): DownloadRowState = DownloadRowState(
    packageName = packageName,
    displayName = displayName,
    iconUrl = downloadIconUrl(this),
    status = status,
    progress = progress,
    speed = speed,
    timeRemaining = timeRemaining,
    downloadedAt = downloadedAt,
    error = error,
    isActive = isActive,
    isFinished = isFinished
)

internal data class DownloadRow(val status: String, val detail: String?)

internal fun downloadRow(context: Context, download: DownloadRowState): DownloadRow = DownloadRow(
    status = statusCaption(context, download.status),
    detail = when (download.status) {
        DownloadStatus.DOWNLOADING, DownloadStatus.INSTALLING -> progressDetail(context, download)

        DownloadStatus.QUEUED, DownloadStatus.VERIFYING -> null

        else ->
            download.error
                ?.let { context.getString(it.stringRes()) }
                ?: download.downloadedAt.takeIf { it > 0L }?.let(CommonUtil::formatDate)
    }
)

private fun progressDetail(context: Context, download: DownloadRowState): String {
    val percent = context.getString(R.string.download_percent, download.progress)
    val rate = CommonUtil.transferRateText(context, download.speed, download.timeRemaining)
    return listOfNotNull(percent, rate).joinToString(SEPARATOR)
}

/** Carries progress inside the caption; the install button has no separate detail line. */
internal fun statusCaption(
    context: Context,
    status: DownloadStatus,
    progress: Int? = null
): String = when (status) {
    DownloadStatus.QUEUED -> context.getString(R.string.download_status_queued)
    DownloadStatus.DOWNLOADING ->
        progress
            ?.let { context.getString(R.string.download_status_downloading, it) }
            ?: context.getString(R.string.download_status_downloading_plain)

    DownloadStatus.VERIFYING -> context.getString(R.string.download_status_verifying)
    DownloadStatus.INSTALLING ->
        progress
            ?.let { context.getString(R.string.download_status_installing, it) }
            ?: context.getString(R.string.download_status_installing_plain)

    DownloadStatus.AWAITING_CONFIRMATION ->
        context.getString(R.string.download_status_awaiting_confirmation)

    DownloadStatus.COMPLETED -> context.getString(R.string.download_status_completed)
    DownloadStatus.INSTALLED -> context.getString(R.string.download_status_installed)
    DownloadStatus.FAILED -> context.getString(R.string.download_status_failed)
    DownloadStatus.CANCELLED -> context.getString(R.string.download_status_cancelled)
    DownloadStatus.UNAVAILABLE -> context.getString(R.string.download_status_unavailable)
}

/** Drawn under the install button, which the downloads list has no room for. */
internal sealed interface ProgressBar {
    data object None : ProgressBar
    data object Indeterminate : ProgressBar
    data class Determinate(val fraction: Float) : ProgressBar

    val isActive: Boolean get() = this != None

    val percent: Float
        get() = when (this) {
            is Determinate -> fraction * PERCENT
            else -> 0F
        }
}

internal fun barFor(progress: Int) =
    ProgressBar.Determinate(progress.coerceIn(0, PERCENT.toInt()) / PERCENT)

/** The immutable server icon URL: an upstream apkUrl has no repo-relative base, so the
 * hash is the only reliable source. */
internal fun downloadIconUrl(download: Download): String =
    ShizuUrls.icon(ServerConfig.baseUrl, download.iconHash).orEmpty()

internal fun progressPercent(progress: Int): Float = progress.coerceIn(0, PERCENT.toInt()).toFloat()

private const val SEPARATOR = " · "

private const val PERCENT = 100F
