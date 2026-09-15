/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's NotificationUtil (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Parcelable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.TimeUnit
import me.timschneeberger.shizustore.MainActivity
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.navigation.Screen
import me.timschneeberger.shizustore.data.receiver.DownloadCancelReceiver
import me.timschneeberger.shizustore.data.receiver.DownloadRetryReceiver
import me.timschneeberger.shizustore.data.room.entity.Download
import me.timschneeberger.shizustore.extensions.isOAndAbove

object NotificationUtil {
    private const val TAG = "NotificationUtil"

    const val CHANNEL_DOWNLOAD = "download"

    const val CHANNEL_INSTALL = "install_v2"

    const val CHANNEL_UPDATE = "update"
    const val CHANNEL_REPO = "repo"
    const val CHANNEL_ALERTS = "alerts"

    private val LEGACY_CHANNELS = listOf("install")

    private const val GROUP_CHANNELS_ACTIVITY = "me.timschneeberger.shizustore.channels.ACTIVITY"
    private const val GROUP_CHANNELS_ALERTS = "me.timschneeberger.shizustore.channels.ALERTS"

    private const val GROUP_INSTALLED = "me.timschneeberger.shizustore.INSTALLED"
    private const val GROUP_FAILED = "me.timschneeberger.shizustore.FAILED"
    private const val GROUP_DOWNLOADS = "me.timschneeberger.shizustore.DOWNLOADS"

    private const val SUMMARY_ID_INSTALLED = 900_001
    private const val SUMMARY_ID_FAILED = 900_002
    private const val SUMMARY_ID_DOWNLOADS = 900_003

    private const val SUMMARY_SEPARATOR = ", "

    private const val NAMED_UPDATES = 3

    private val INSTALLED_TIMEOUT_MS = TimeUnit.HOURS.toMillis(6)

    const val UPDATES_NOTIFICATION_ID = 300

    fun notificationId(packageName: String): Int = packageName.hashCode()

    fun createChannels(context: Context) {
        if (!isOAndAbove) return

        val manager = context.getSystemService<NotificationManager>() ?: return

        LEGACY_CHANNELS.forEach { manager.deleteNotificationChannel(it) }

        manager.createNotificationChannelGroups(
            listOf(
                NotificationChannelGroup(
                    GROUP_CHANNELS_ACTIVITY,
                    context.getString(R.string.notification_group_activity)
                ),
                NotificationChannelGroup(
                    GROUP_CHANNELS_ALERTS,
                    context.getString(R.string.notification_group_alerts)
                )
            )
        )

        manager.createNotificationChannels(
            listOf(
                channel(
                    context,
                    CHANNEL_DOWNLOAD,
                    R.string.notification_channel_download,
                    NotificationManager.IMPORTANCE_LOW,
                    GROUP_CHANNELS_ACTIVITY
                ),
                channel(
                    context,
                    CHANNEL_INSTALL,
                    R.string.notification_channel_install,
                    NotificationManager.IMPORTANCE_LOW,
                    GROUP_CHANNELS_ACTIVITY
                ),
                channel(
                    context,
                    CHANNEL_UPDATE,
                    R.string.notification_channel_update,
                    NotificationManager.IMPORTANCE_DEFAULT,
                    GROUP_CHANNELS_ACTIVITY
                ),
                channel(
                    context,
                    CHANNEL_ALERTS,
                    R.string.notification_channel_alerts,
                    NotificationManager.IMPORTANCE_HIGH,
                    GROUP_CHANNELS_ALERTS
                ),
                channel(
                    context,
                    CHANNEL_REPO,
                    R.string.notification_channel_repo,
                    NotificationManager.IMPORTANCE_HIGH,
                    GROUP_CHANNELS_ALERTS
                )
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun channel(
        context: Context,
        id: String,
        @StringRes nameRes: Int,
        importance: Int,
        group: String
    ) = NotificationChannel(id, context.getString(nameRes), importance).apply {
        this.group = group
        if (group == GROUP_CHANNELS_ACTIVITY) setSound(null, null)
    }

    fun downloadNotification(
        context: Context,
        download: Download?,
        grouped: Boolean = false
    ): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(
                download?.displayName?.let {
                    context.getString(R.string.notification_downloading, it)
                } ?: context.getString(R.string.app_name)
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(context))
            .apply {
                // A lone download stays ungrouped so its progress bar stays visible.
                if (grouped) {
                    setGroup(GROUP_DOWNLOADS)
                    setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                }
            }

        if (download == null) return builder.setProgress(100, 0, true).build()

        return builder
            .setContentText(progressText(context, download))
            .setProgress(100, download.progress, download.progress <= 0)
            .addAction(
                R.drawable.ic_cancel,
                context.getString(R.string.action_cancel),
                broadcast(
                    context,
                    download.packageName,
                    DownloadCancelReceiver::class.java,
                    DownloadCancelReceiver.ACTION_CANCEL
                )
            )
            .build()
    }

    fun completedNotification(context: Context, download: Download): Notification =
        NotificationCompat.Builder(context, CHANNEL_INSTALL)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle(
                context.getString(R.string.notification_download_complete, download.displayName)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(appDetailsIntent(context, download.packageName))
            .build()

    fun failedNotification(context: Context, download: Download): Notification =
        NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_cancel)
            .setContentTitle(
                context.getString(R.string.notification_download_failed, download.displayName)
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setGroup(GROUP_FAILED)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setAutoCancel(true)
            .setContentIntent(appDetailsIntent(context, download.packageName))
            .addAction(retryAction(context, download.packageName))
            .build()

    /** Replaces the stale "downloaded" notification while the installer runs. */
    fun installingNotification(
        context: Context,
        packageName: String,
        displayName: String
    ): Notification = NotificationCompat.Builder(context, CHANNEL_INSTALL)
        .setSmallIcon(R.drawable.ic_install_done)
        .setContentTitle(
            context.getString(R.string.notification_installing, displayName)
        )
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setProgress(100, 0, true)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(appDetailsIntent(context, packageName))
        .build()

    fun installedNotification(
        context: Context,
        packageName: String,
        displayName: String
    ): Notification = NotificationCompat.Builder(context, CHANNEL_INSTALL)
        .setSmallIcon(R.drawable.ic_install_done)
        .setLargeIcon(appIcon(context, packageName))
        .setContentTitle(
            context.getString(R.string.notification_install_complete, displayName)
        )
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setGroup(GROUP_INSTALLED)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        .setAutoCancel(true)
        .setTimeoutAfter(INSTALLED_TIMEOUT_MS)
        .setContentIntent(appDetailsIntent(context, packageName))
        .build()

    fun installFailedNotification(
        context: Context,
        packageName: String,
        displayName: String,
        reason: String?
    ): Notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
        .setSmallIcon(R.drawable.ic_cancel)
        .setContentTitle(
            context.getString(R.string.notification_install_failed, displayName)
        )
        .setContentText(reason)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ERROR)
        .setGroup(GROUP_FAILED)
        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
        .setAutoCancel(true)
        .setContentIntent(appDetailsIntent(context, packageName))
        .addAction(retryAction(context, packageName))
        .build()

    fun notifyApp(context: Context, packageName: String, notification: Notification) {
        val id = notificationId(packageName)
        notify(context, id, notification)
        refreshGroupSummaries(context, AppNotification.of(id, notification))
    }

    fun clearAppNotification(context: Context, packageName: String) {
        val id = notificationId(packageName)
        cancel(context, id)
        refreshGroupSummaries(context, AppNotification.gone(id))
    }

    private data class AppNotification(val id: Int, val group: String?, val title: String?) {
        companion object {
            fun of(id: Int, notification: Notification) = AppNotification(
                id = id,
                group = notification.group,
                title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            )

            fun gone(id: Int) = AppNotification(id, group = null, title = null)
        }
    }

    fun refreshGroupSummaries(context: Context) = refreshGroupSummaries(context, change = null)

    private fun refreshGroupSummaries(context: Context, change: AppNotification?) {
        val manager = context.getSystemService<NotificationManager>() ?: return

        runCatching {
            refreshSummary(
                context = context,
                children = childrenOf(manager, GROUP_INSTALLED, SUMMARY_ID_INSTALLED, change),
                summaryId = SUMMARY_ID_INSTALLED,
                group = GROUP_INSTALLED,
                channelId = CHANNEL_INSTALL,
                smallIcon = R.drawable.ic_install_done,
                titleRes = R.plurals.notification_installed_summary,
                timeoutMs = INSTALLED_TIMEOUT_MS
            )
            refreshSummary(
                context = context,
                children = childrenOf(manager, GROUP_FAILED, SUMMARY_ID_FAILED, change),
                summaryId = SUMMARY_ID_FAILED,
                group = GROUP_FAILED,
                channelId = CHANNEL_ALERTS,
                smallIcon = R.drawable.ic_cancel,
                titleRes = R.plurals.notification_failed_summary,
                timeoutMs = null
            )
            refreshSummary(
                context = context,
                children = childrenOf(manager, GROUP_DOWNLOADS, SUMMARY_ID_DOWNLOADS, change),
                summaryId = SUMMARY_ID_DOWNLOADS,
                group = GROUP_DOWNLOADS,
                channelId = CHANNEL_DOWNLOAD,
                smallIcon = android.R.drawable.stat_sys_download,
                titleRes = R.plurals.notification_download_summary,
                timeoutMs = null,
                ongoing = true,
                initialTab = MainActivity.TAB_APPS
            )
        }.onFailure { Log.w(TAG, "Could not refresh the notification summaries", it) }
    }

    private fun childrenOf(
        manager: NotificationManager,
        group: String,
        summaryId: Int,
        change: AppNotification?
    ): List<AppNotification> {
        val byId = manager.activeNotifications
            .filter { it.id != summaryId }
            .associate { it.id to AppNotification.of(it.id, it.notification) }
            .toMutableMap()

        change?.let { byId[it.id] = it }

        return byId.values.filter { it.group == group }
    }

    private fun refreshSummary(
        context: Context,
        children: List<AppNotification>,
        summaryId: Int,
        group: String,
        channelId: String,
        @DrawableRes smallIcon: Int,
        @PluralsRes titleRes: Int,
        timeoutMs: Long?,
        ongoing: Boolean = false,
        initialTab: Int = MainActivity.TAB_UPDATES
    ) {
        if (children.isEmpty()) {
            cancel(context, summaryId)
            return
        }

        val titles = children.mapNotNull { it.title }
        val title = context.resources.getQuantityString(titleRes, children.size, children.size)

        val summary = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(titles.joinToString(SUMMARY_SEPARATOR))
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle(title)
                    .also { style -> titles.forEach(style::addLine) }
            )
            .setGroup(group)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setAutoCancel(true)
            .setOngoing(ongoing)
            .setContentIntent(openAppIntent(context, initialTab))
            .apply { timeoutMs?.let { setTimeoutAfter(it) } }
            .build()

        notify(context, summaryId, summary)
    }

    private fun updatesText(context: Context, names: List<String>): String = when {
        names.isEmpty() -> context.getString(R.string.notification_updates_available_text)
        names.size == 1 -> names[0]

        names.size == 2 -> context.getString(
            R.string.notification_updates_available_desc_2,
            names[0],
            names[1]
        )

        names.size == 3 -> context.getString(
            R.string.notification_updates_available_desc_3,
            names[0],
            names[1],
            names[2]
        )

        else -> context.resources.getQuantityString(
            R.plurals.notification_updates_available_desc_more,
            names.size - NAMED_UPDATES,
            names[0],
            names[1],
            names[2],
            names.size - NAMED_UPDATES
        )
    }

    private fun retryAction(context: Context, packageName: String): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            R.drawable.ic_updates,
            context.getString(R.string.action_retry),
            broadcast(
                context,
                packageName,
                DownloadRetryReceiver::class.java,
                DownloadRetryReceiver.ACTION_RETRY
            )
        ).build()

    private fun appIcon(context: Context, packageName: String): Bitmap? = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
            .loadIcon(context.packageManager)
            .toBitmap()
    }.getOrNull()

    fun installRefusedNotification(context: Context, displayName: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_cancel)
            .setContentTitle(
                context.getString(R.string.notification_install_refused, displayName)
            )
            .setContentText(context.getString(R.string.notification_install_refused_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setGroup(GROUP_FAILED)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()

    fun confirmInstallNotification(
        context: Context,
        packageName: String,
        displayName: String,
        confirmIntent: Intent
    ): Notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
        .setSmallIcon(R.drawable.ic_install_done)
        .setContentTitle(
            context.getString(R.string.notification_confirm_install, displayName)
        )
        .setContentText(context.getString(R.string.notification_confirm_install_text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setOngoing(true)
        .setAutoCancel(true)
        .setContentIntent(
            PendingIntent.getActivity(
                context,
                notificationId(packageName),
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    fun updatesAvailableNotification(context: Context, names: List<String>): Notification =
        NotificationCompat.Builder(context, CHANNEL_UPDATE)
            .setSmallIcon(R.drawable.ic_updates)
            .setContentTitle(
                context.resources.getQuantityString(
                    R.plurals.notification_updates_available,
                    names.size,
                    names.size
                )
            )
            .setContentText(updatesText(context, names))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context, MainActivity.TAB_UPDATES))
            .build()

    fun fingerprintMismatchNotification(context: Context, repoName: String): Notification {
        val text = context.getString(R.string.notification_fingerprint_mismatch_text)
        return NotificationCompat.Builder(context, CHANNEL_REPO)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(
                context.getString(R.string.notification_fingerprint_mismatch, repoName)
            )
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
    }

    /** POST_NOTIFICATIONS may be absent; a refused post must not fail the work around it. */
    @SuppressLint("MissingPermission")
    fun notify(context: Context, id: Int, notification: Notification) {
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
            .onFailure { Log.w(TAG, "Could not post notification $id", it) }
    }

    fun cancel(context: Context, id: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(id) }
            .onFailure { Log.w(TAG, "Could not cancel notification $id", it) }
    }

    private fun progressText(context: Context, download: Download): String? =
        CommonUtil.transferRateText(context, download.speed, download.timeRemaining)

    private fun openAppIntent(
        context: Context,
        initialTab: Int = MainActivity.TAB_APPS
    ): PendingIntent = PendingIntent.getActivity(
        context,
        initialTab,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_INITIAL_TAB, initialTab),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun appDetailsIntent(context: Context, packageName: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            notificationId(packageName),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(
                    MainActivity.EXTRA_SCREEN,
                    Screen.AppDetails(packageName) as Parcelable
                ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun broadcast(
        context: Context,
        packageName: String,
        receiver: Class<*>,
        action: String
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        notificationId(packageName),
        Intent(context, receiver).apply {
            this.action = action
            putExtra(DownloadCancelReceiver.EXTRA_PACKAGE_NAME, packageName)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
