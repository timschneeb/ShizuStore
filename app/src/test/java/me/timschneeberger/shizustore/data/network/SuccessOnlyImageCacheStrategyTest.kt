/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.network

import android.os.Build
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.request.Options
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM])
class SuccessOnlyImageCacheStrategyTest {

    private val strategy = SuccessOnlyImageCacheStrategy()
    private val options = Options(context = RuntimeEnvironment.getApplication())
    private val request = NetworkRequest(url = "http://localhost/icons/abc.png")

    @Test
    fun cachedSuccessIsReused() = runTest {
        val cached = NetworkResponse(code = 200)

        val result = strategy.read(cached, request, options)

        assertSame(cached, result.response)
    }

    @Test
    fun cachedFailureIsRevalidated() = runTest {
        val result = strategy.read(NetworkResponse(code = 404), request, options)

        assertNull(result.response)
        assertSame(request, result.request)
    }

    @Test
    fun successIsWrittenToDisk() = runTest {
        val response = NetworkResponse(code = 200)

        val result = strategy.write(null, request, response, options)

        assertSame(response, result.response)
    }

    @Test
    fun failureIsNotWrittenToDisk() = runTest {
        val result = strategy.write(null, request, NetworkResponse(code = 404), options)

        assertNull(result.response)
    }

    @Test
    fun onlyTwoHundredsAreCacheable() {
        assertTrue(isCacheableResponse(200))
        assertTrue(isCacheableResponse(204))
        assertFalse(isCacheableResponse(304))
        assertFalse(isCacheableResponse(404))
        assertFalse(isCacheableResponse(500))
    }
}
