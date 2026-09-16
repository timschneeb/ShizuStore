/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore

import me.timschneeberger.shizustore.data.api.BaseUrlProvider
import me.timschneeberger.shizustore.data.api.OkHttpShizuApi
import me.timschneeberger.shizustore.data.api.RequestThrottle
import me.timschneeberger.shizustore.data.api.ShizuJson
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before

abstract class ApiTestBase : RobolectricTestBase() {

    protected lateinit var server: MockWebServer

    @Before
    fun startServer() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun stopServer() {
        server.shutdown()
    }

    protected fun api(): OkHttpShizuApi = OkHttpShizuApi(
        client = OkHttpClient(),
        json = ShizuJson,
        baseUrlProvider = BaseUrlProvider({ server.url("/").toString() }, defaultValue = ""),
        throttle = RequestThrottle(minSpacingMillis = 0)
    )
}
