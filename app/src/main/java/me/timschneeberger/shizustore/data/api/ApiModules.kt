/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.api

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import me.timschneeberger.shizustore.util.Preferences
import me.timschneeberger.shizustore.util.ServerConfig

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun providesJson(): Json = ShizuJson

    @Provides
    @Singleton
    fun providesBaseUrlProvider(@ApplicationContext context: Context): BaseUrlProvider =
        BaseUrlProvider(
            readOverride = {
                val useCustom = Preferences.readBoolean(
                    context,
                    Preferences.PREFERENCE_API_SERVER_CUSTOM
                )
                if (useCustom) {
                    Preferences.readString(context, Preferences.PREFERENCE_API_BASE_URL)
                        .takeIf { it.isNotBlank() }
                } else {
                    null
                }
            },
            observeOverride = {
                combine(
                    Preferences.booleanFlow(context, Preferences.PREFERENCE_API_SERVER_CUSTOM),
                    Preferences.stringFlow(context, Preferences.PREFERENCE_API_BASE_URL, "")
                ) { useCustom, customUrl -> if (useCustom) customUrl else "" }
            },
            defaultValue = ServerConfig.productionBaseUrl
        )

    @Provides
    @Singleton
    fun providesRequestThrottle(): RequestThrottle = RequestThrottle()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ShizuApiModule {
    @Binds
    @Singleton
    abstract fun bindShizuApi(impl: OkHttpShizuApi): ShizuApi
}
