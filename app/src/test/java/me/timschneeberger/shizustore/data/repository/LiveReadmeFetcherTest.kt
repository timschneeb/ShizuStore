/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.repository

import kotlinx.coroutines.test.runTest
import me.timschneeberger.shizustore.ApiTestBase
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LiveReadmeFetcherTest : ApiTestBase() {

    private lateinit var fetcher: LiveReadmeFetcher

    @Before
    fun setUp() {
        fetcher = LiveReadmeFetcher(OkHttpClient())
    }

    @Test
    fun fetchReturnsRawMarkdownAndBypassesCaches() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("# Live\n"))

        val markdown = fetcher.fetch(server.url("/README.md").toString())

        assertEquals("# Live\n", markdown)
        val request = server.takeRequest()
        assertEquals("/README.md", request.path)
        assertTrue(request.getHeader("Cache-Control")!!.contains("no-cache"))
    }

    @Test
    fun fetchFailsSoftOnHttpError() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        assertNull(fetcher.fetch(server.url("/README.md").toString()))
    }

    @Test
    fun fetchRejectsBlankBody() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("   \n"))

        assertNull(fetcher.fetch(server.url("/README.md").toString()))
    }

    @Test
    fun fetchRejectsNonHttpSchemes() = runTest {
        assertNull(fetcher.fetch("file:///etc/hosts"))
    }

    @Test
    fun fetchCapsOversizedBody() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("x".repeat(MAX_BYTES + 100))
        )

        assertEquals(MAX_BYTES, fetcher.fetch(server.url("/README.md").toString())?.length)
    }

    private companion object {
        const val MAX_BYTES = 2 * 1024 * 1024
    }
}
