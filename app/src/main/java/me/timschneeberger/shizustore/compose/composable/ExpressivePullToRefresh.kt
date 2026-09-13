/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

private const val MIN_REFRESH_VISIBLE_MS = 500L

/** Pull to refresh using the Material 3 expressive loading indicator. While the
 * indicator is visible the content is replaced by [placeholder] (a skeleton),
 * or blanked when none is given, so stale rows do not show through. A fast sync
 * completes in a blink, which reads as "nothing happened", so the feedback is
 * held for at least [MIN_REFRESH_VISIBLE_MS]. */
@Composable
fun ExpressivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    val visible = rememberVisibleForAtLeast(isRefreshing, MIN_REFRESH_VISIBLE_MS)
    Box(modifier) {
        PullToRefreshBox(
            isRefreshing = visible,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = state,
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    state = state,
                    isRefreshing = visible,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            content()
            if (visible) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    placeholder?.invoke(this)
                }
            }
        }
    }
}

/** Reports [loading], but stays true until it has been visible for [minMillis]. */
@Composable
internal fun rememberVisibleForAtLeast(loading: Boolean, minMillis: Long): Boolean {
    var visible by remember { mutableStateOf(loading) }
    var startedAt by remember { mutableStateOf(0L) }
    LaunchedEffect(loading) {
        if (loading) {
            startedAt = SystemClock.elapsedRealtime()
            visible = true
        } else if (visible) {
            val remaining = minMillis - (SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0) delay(remaining)
            visible = false
        }
    }
    return loading || visible
}
