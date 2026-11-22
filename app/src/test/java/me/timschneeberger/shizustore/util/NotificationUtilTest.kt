/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import android.os.Build
import androidx.core.content.IntentCompat
import me.timschneeberger.shizustore.MainActivity
import me.timschneeberger.shizustore.compose.navigation.Screen
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.room.entity.Download
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class NotificationUtilTest {

    @Test
    fun completedDownloadOpensTheListingItCameFrom() {
        val context = RuntimeEnvironment.getApplication()
        val download = Download(
            packageName = "com.shared",
            repoId = -1,
            versionCode = 8,
            displayName = "Fork B",
            iconUrl = null,
            size = 1024,
            status = DownloadStatus.COMPLETED,
            apkUrl = "https://example/fork-b.apk",
            apkName = "fork-b.apk",
            hash = "hash",
            signer = "bbbbbbbb",
            appSlug = "fork-b"
        )

        val notification = NotificationUtil.completedNotification(context, download)
        val intent = shadowOf(notification.contentIntent).savedIntent
        val screen = IntentCompat.getParcelableExtra(
            intent,
            MainActivity.EXTRA_SCREEN,
            Screen::class.java
        )

        assertEquals(Screen.AppDetails("fork-b"), screen)
    }

    @Test
    fun legacyDownloadWithoutProvenanceFallsBackToItsPackage() {
        val context = RuntimeEnvironment.getApplication()
        val download = Download(
            packageName = "com.shared",
            repoId = -1,
            versionCode = 8,
            displayName = "Legacy",
            iconUrl = null,
            size = 1024,
            status = DownloadStatus.COMPLETED,
            apkUrl = "https://example/legacy.apk",
            apkName = "legacy.apk",
            hash = "hash",
            signer = null,
            appSlug = null
        )

        val notification = NotificationUtil.completedNotification(context, download)
        val intent = shadowOf(notification.contentIntent).savedIntent
        val screen = IntentCompat.getParcelableExtra(
            intent,
            MainActivity.EXTRA_SCREEN,
            Screen::class.java
        )

        assertEquals(Screen.AppDetails("com.shared"), screen)
    }
}
