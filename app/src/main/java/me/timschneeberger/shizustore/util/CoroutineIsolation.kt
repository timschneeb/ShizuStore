/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.util

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

fun isolatedIoScope(tag: String): CoroutineScope = CoroutineScope(
    SupervisorJob() +
        Dispatchers.IO +
        CoroutineExceptionHandler { _, throwable ->
            Log.e(tag, "Uncaught failure on $tag's IO scope", throwable)
        }
)

inline fun isolate(tag: String, what: String, block: () -> Unit): Boolean {
    try {
        block()
    } catch (throwable: Throwable) {
        Log.e(tag, "Failed to $what", throwable)
        return false
    }
    return true
}
