/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

sealed class ViewState {
    inline fun <reified T> ViewState.getDataAs(): T = (this as? Success<*>)?.data as T

    data object Loading : ViewState()
    data object Empty : ViewState()
    data class Error(val error: String?) : ViewState()
    data class Status(val status: String?) : ViewState()
    data class Success<T>(val data: T) : ViewState()
}

sealed class AuthState {
    data object Init : AuthState()
    data object Available : AuthState()
    data object Unavailable : AuthState()
    data object SignedIn : AuthState()
    data object SignedOut : AuthState()
    data object Valid : AuthState()
    data object Fetching : AuthState()
    data object Verifying : AuthState()
    data class PendingAccountManager(val email: String, val token: String) : AuthState()
    data class Failed(val status: String) : AuthState()
}

sealed class AppState {
    data class Downloading(
        val progress: Float,
        val speed: Long,
        val timeRemaining: Long
    ) : AppState()

    data object Queued : AppState()
    data object Purchasing : AppState()
    data object Verifying : AppState()
    data class Installing(val progress: Float) : AppState()
    data class Error(val message: String?) : AppState()
    data class Installed(val versionName: String, val versionCode: Long) : AppState()
    data object Archived : AppState()
    data object Updatable : AppState()
    data object Unavailable : AppState()
    data object Loading : AppState()

    fun inProgress(): Boolean = this is Downloading ||
        this is Installing ||
        this is Purchasing ||
        this is Queued ||
        this is Verifying

    fun progress(): Float = when (this) {
        is Downloading -> progress
        else -> 0F
    }
}
