/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.helper

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import me.timschneeberger.shizustore.ApiTestBase
import me.timschneeberger.shizustore.data.room.entity.AppEntity
import me.timschneeberger.shizustore.util.Preferences
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class InstallReporterTest : ApiTestBase() {

    private lateinit var reporter: InstallReporter

    @Before
    fun setUp() {
        reporter = InstallReporter(
            context = RuntimeEnvironment.getApplication(),
            appDao = db.appDao(),
            api = api()
        )
        runBlocking {
            Preferences.putBoolean(
                RuntimeEnvironment.getApplication(),
                Preferences.PREFERENCE_INSTALL_REPORTING,
                true
            )
        }
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
    fun disabledReportingSendsNothing() = runTest {
        Preferences.putBoolean(
            RuntimeEnvironment.getApplication(),
            Preferences.PREFERENCE_INSTALL_REPORTING,
            false
        )
        db.appDao().upsert(
            AppEntity(
                slug = "micup",
                name = "MicUp",
                description = "desc",
                packageName = "com.example.micup"
            )
        )

        reporter.reportInstalled("com.example.micup", 42L)

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
