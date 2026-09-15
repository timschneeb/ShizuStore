/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import android.os.Build
import androidx.room.Room
import kotlinx.coroutines.test.runTest
import me.timschneeberger.shizustore.data.api.BaseUrlProvider
import me.timschneeberger.shizustore.data.api.OkHttpShizuApi
import me.timschneeberger.shizustore.data.api.RequestThrottle
import me.timschneeberger.shizustore.data.api.ShizuJson
import me.timschneeberger.shizustore.data.room.AuroraDatabase
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class InstallReporterTest {

    private lateinit var server: MockWebServer
    private lateinit var db: AuroraDatabase
    private lateinit var reporter: InstallReporter

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            AuroraDatabase::class.java
        ).allowMainThreadQueries().build()

        val api = OkHttpShizuApi(
            client = OkHttpClient(),
            json = ShizuJson,
            baseUrlProvider = BaseUrlProvider({ server.url("/").toString() }, defaultValue = ""),
            throttle = RequestThrottle(minSpacingMillis = 0)
        )
        reporter = InstallReporter(appDao = db.appDao(), api = api)
    }

    @After
    fun tearDown() {
        db.close()
        server.shutdown()
    }

    @Test
    fun postsSlugAndReportsServerTotal() = runTest {
        db.appDao().upsert(
            AppEntity(
                slug = "micup",
                name = "MicUp",
                description = "desc",
                packageName = "com.example.micup"
            )
        )
        server.enqueue(MockResponse().setBody("""{"slug":"micup","installCount":7}"""))

        reporter.reportInstalled("com.example.micup", 42L)

        val request = server.takeRequest()
        assertEquals("/v1/apps/micup/installs", request.path)
        assertEquals("POST", request.method)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun samePackageVersionReportsOnce() = runTest {
        db.appDao().upsert(
            AppEntity(
                slug = "micup",
                name = "MicUp",
                description = "desc",
                packageName = "com.example.micup"
            )
        )
        server.enqueue(MockResponse().setBody("""{"slug":"micup","installCount":1}"""))
        server.enqueue(MockResponse().setBody("""{"slug":"micup","installCount":2}"""))

        reporter.reportInstalled("com.example.micup", 42L)
        reporter.reportInstalled("com.example.micup", 42L)

        assertEquals(1, server.requestCount)
    }

    @Test
    fun unknownPackageSendsNothing() = runTest {
        reporter.reportInstalled("com.example.ghost", 1L)

        assertEquals(0, server.requestCount)
    }

    @Test
    fun serverErrorNeverThrows() = runTest {
        db.appDao().upsert(
            AppEntity(
                slug = "micup",
                name = "MicUp",
                description = "desc",
                packageName = "com.example.micup"
            )
        )
        server.enqueue(MockResponse().setResponseCode(500))

        reporter.reportInstalled("com.example.micup", 42L)

        assertEquals(1, server.requestCount)
    }
}
