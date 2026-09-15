/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.timschneeberger.shizustore.data.installer.HiddenApiExemption
import me.timschneeberger.shizustore.data.installer.ShizukuInstaller
import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.data.network.SuccessOnlyImageCacheStrategy
import me.timschneeberger.shizustore.data.receiver.PackageManagerReceiver
import me.timschneeberger.shizustore.data.work.UpdateWorker
import me.timschneeberger.shizustore.extensions.isPAndAbove
import me.timschneeberger.shizustore.util.NotificationUtil
import me.timschneeberger.shizustore.util.Preferences
import me.timschneeberger.shizustore.util.isolatedIoScope
import okhttp3.OkHttpClient
import org.lsposed.hiddenapibypass.HiddenApiBypass

@HiltAndroidApp
class ShizuApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var okHttpClient: OkHttpClient

    private val appScope = isolatedIoScope(TAG)

    @OptIn(ExperimentalCoilApi::class)
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { okHttpClient },
                        cacheStrategy = { SuccessOnlyImageCacheStrategy() }
                    )
                )
                // README badges and wordmarks are frequently SVG.
                add(SvgDecoder.Factory())
            }
            .build()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        exemptPackageManagerHiddenApis()
        NotificationUtil.createChannels(this)
        runBlocking { UpdateWorker.schedule(this@ShizuApp) }
        registerPackageManagerReceiver()
        selectShizukuInstallerOnFirstLaunch()
    }

    /**
     * A data reset clears the installer choice while Shizuku keeps its grant, so
     * pick Shizuku once when it is available and granted and otherwise persist
     * the default. Running only while the preference is absent keeps this a
     * one-time first-launch step.
     */
    private fun selectShizukuInstallerOnFirstLaunch() {
        appScope.launch {
            val stored = Preferences.readInteger(
                this@ShizuApp,
                Preferences.PREFERENCE_INSTALLER_ID,
                NO_INSTALLER_CHOSEN
            )
            if (stored != NO_INSTALLER_CHOSEN) return@launch

            val installer = if (
                ShizukuInstaller.isAvailable(this@ShizuApp) &&
                ShizukuInstaller.hasPermission()
            ) {
                Installer.SHIZUKU
            } else {
                Installer.SESSION
            }

            Preferences.putInteger(
                this@ShizuApp,
                Preferences.PREFERENCE_INSTALLER_ID,
                installer.ordinal
            )
        }
    }

    private fun exemptPackageManagerHiddenApis() {
        if (!isPAndAbove) return

        val exempted = runCatching {
            HiddenApiBypass.addHiddenApiExemptions(*PACKAGE_INSTALLER_SIGNATURE_PREFIXES)
        }.getOrElse { failure ->
            Log.w(TAG, "Hidden-API bypass could not initialise on this runtime", failure)
            false
        }

        HiddenApiExemption.record(exempted)

        if (!exempted) {
            Log.w(TAG, "Hidden-API exemption refused; Shizuku installs will fail on this device")
        }
    }

    private fun registerPackageManagerReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }

        ContextCompat.registerReceiver(
            this,
            PackageManagerReceiver(),
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private companion object {
        const val TAG = "ShizuApp"

        // Sentinel that means no installer has been persisted yet.
        const val NO_INSTALLER_CHOSEN = -1

        val PACKAGE_INSTALLER_SIGNATURE_PREFIXES = arrayOf(
            "Landroid/content/pm/IPackageManager",
            "Landroid/content/pm/IPackageInstaller",
            "Landroid/content/pm/PackageInstaller",
            "Landroid/content/pm/PackageManager;->INSTALL_"
        )
    }
}
