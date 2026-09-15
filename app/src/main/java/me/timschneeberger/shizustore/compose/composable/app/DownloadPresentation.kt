/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * The downloads row treatment is Aurora Store's, read from its DownloadListItem.
 */

package me.timschneeberger.shizustore.compose.composable.app

import android.content.Context
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.stringRes
import me.timschneeberger.shizustore.compose.indexUrl
import me.timschneeberger.shizustore.data.api.ShizuUrls
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.util.CommonUtil
import me.timschneeberger.shizustore.util.ServerConfig

/**
 * The two lines under a download's name: what it is doing, and how it is going.
 *
 * One in flight reports progress, speed and time left; one that has finished has nothing left to
 * report, so it gives why it failed, or the date it arrived.
 */
internal data class DownloadRow(val status: String, val detail: String?)

internal fun downloadRow(context: Context, download: Download): DownloadRow = DownloadRow(
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

private fun progressDetail(context: Context, download: Download): String {
    val percent = context.getString(R.string.download_percent, download.progress)
    val rate = CommonUtil.transferRateText(context, download.speed, download.timeRemaining)
    return listOfNotNull(percent, rate).joinToString(SEPARATOR)
}

/**
 * The install button has one line to work with, so it asks for the progress inside the caption.
 * The downloads list leaves it out and puts progress, speed and time left on a line of its own.
 */
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

/** What the install button draws under itself, which the downloads list has no room for. */
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

internal fun iconBaseUrl(download: Download): String =
    download.apkUrl.removeSuffix(download.apkName.trimStart('/')).trimEnd('/')

/**
 * Catalog downloads carry the server icon hash but no repo icon URL, and their
 * apkUrl points upstream, so the legacy repo-relative base cannot resolve them.
 * Prefer the immutable server icon URL and keep the legacy path as fallback.
 */
internal fun downloadIconUrl(download: Download): String =
    ShizuUrls.icon(ServerConfig.baseUrl, download.iconHash)
        ?: indexUrl(iconBaseUrl(download), download.iconUrl).orEmpty()

internal fun progressPercent(progress: Int): Float = progress.coerceIn(0, PERCENT.toInt()).toFloat()

private const val SEPARATOR = " · "

private const val PERCENT = 100F
