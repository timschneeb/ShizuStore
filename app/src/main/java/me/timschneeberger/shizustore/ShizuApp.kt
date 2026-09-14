/*
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
import kotlinx.coroutines.runBlocking
import me.timschneeberger.shizustore.data.installer.HiddenApiExemption
import me.timschneeberger.shizustore.data.network.SuccessOnlyImageCacheStrategy
import me.timschneeberger.shizustore.data.receiver.PackageManagerReceiver
import me.timschneeberger.shizustore.data.work.UpdateWorker
import me.timschneeberger.shizustore.extensions.isPAndAbove
import me.timschneeberger.shizustore.util.NotificationUtil
import okhttp3.OkHttpClient
import org.lsposed.hiddenapibypass.HiddenApiBypass

@HiltAndroidApp
class ShizuApp : Application(), Configuration.Provider, SingletonImageLoader.Factory {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var okHttpClient: OkHttpClient

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

        val PACKAGE_INSTALLER_SIGNATURE_PREFIXES = arrayOf(
            "Landroid/content/pm/IPackageManager",
            "Landroid/content/pm/IPackageInstaller",
            "Landroid/content/pm/PackageInstaller",
            "Landroid/content/pm/PackageManager;->INSTALL_"
        )
    }
}
