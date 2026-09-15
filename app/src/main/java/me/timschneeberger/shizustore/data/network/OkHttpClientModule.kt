/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Aurora Store's OkHttpClientModule.
 */

package me.timschneeberger.shizustore.data.network

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import java.net.Authenticator
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import me.timschneeberger.shizustore.BuildConfig
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.model.ProxyInfo
import me.timschneeberger.shizustore.util.Preferences
import me.timschneeberger.shizustore.util.Preferences.PREFERENCE_PROXY_INFO
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor

internal fun proxyTypeFor(protocol: String): Proxy.Type =
    if (protocol.removeSuffix("5") == "SOCKS") Proxy.Type.SOCKS else Proxy.Type.HTTP

/**
 * Value for the `User-Agent` header sent on every request of the shared
 * client (API, APK downloads, icons). Identifies the app and its version to
 * upstream hosts; spaces are legal inside a header value.
 */
internal fun userAgent(appName: String, versionName: String): String = "$appName/$versionName"

internal class PreferenceProxySelector(
    private val rawProxyInfo: suspend () -> String
) : ProxySelector() {
    override fun select(uri: URI?): List<Proxy> {
        requireNotNull(uri) { "URI can't be null." }
        val info = current() ?: return platformDefault()?.select(uri) ?: NO_PROXY_ONLY
        val address = InetSocketAddress.createUnresolved(info.host, info.port)
        return listOf(Proxy(proxyTypeFor(info.protocol), address))
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {
        require(uri != null && sa != null && ioe != null) { "Arguments can't be null." }
        platformDefault()?.connectFailed(uri, sa, ioe)
    }

    fun credentialsFor(
        requestorType: Authenticator.RequestorType,
        host: String?,
        port: Int
    ): PasswordAuthentication? {
        if (requestorType != Authenticator.RequestorType.PROXY) return null

        val info = current() ?: return null
        if (!host.equals(info.host, ignoreCase = true) || port != info.port) return null

        val proxyUser = info.proxyUser
        val proxyPassword = info.proxyPassword
        if (proxyUser.isNullOrBlank() || proxyPassword.isNullOrBlank()) return null

        return PasswordAuthentication(proxyUser, proxyPassword.toCharArray())
    }

    private fun current(): ProxyInfo? {
        val raw = runBlocking { rawProxyInfo() }
        if (raw.isBlank()) return null
        return runCatching { Json.decodeFromString<ProxyInfo>(raw) }.getOrNull()
    }

    private fun platformDefault(): ProxySelector? = getDefault()?.takeIf { it !== this }

    private companion object {
        val NO_PROXY_ONLY: List<Proxy> = listOf(Proxy.NO_PROXY)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object OkHttpClientModule {
    @Provides
    @Singleton
    fun providesCache(@ApplicationContext context: Context): Cache =
        Cache(File(context.cacheDir, "http"), CACHE_SIZE_BYTES)

    @Provides
    @Singleton
    fun providesProxySelector(@ApplicationContext context: Context): ProxySelector {
        val selector = PreferenceProxySelector {
            Preferences.readString(context, PREFERENCE_PROXY_INFO)
        }

        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication? =
                selector.credentialsFor(requestorType, requestingHost, requestingPort)
        })

        return selector
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(
        @ApplicationContext context: Context,
        proxySelector: ProxySelector,
        cache: Cache
    ): OkHttpClient = OkHttpClient.Builder()
        .cache(cache)
        // Negotiates br (falling back to gzip) for the catalog JSON.
        .addInterceptor(BrotliInterceptor)
        // Single choke point: this client serves the API, APK downloads
        // and Coil, so one header covers every request the app makes.
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header(
                    "User-Agent",
                    userAgent(context.getString(R.string.app_name), BuildConfig.VERSION_NAME)
                )
                .build()
            chain.proceed(request)
        }
        .proxySelector(proxySelector)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val CACHE_SIZE_BYTES = 50L * 1024 * 1024
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DownloaderModule {
    @Binds
    @Singleton
    abstract fun bindDownloader(impl: OkHttpDownloader): Downloader
}
