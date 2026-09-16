/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Droid-ify (GPL-3.0-or-later).
 */

package me.timschneeberger.shizustore.data.network

sealed interface NetworkResponse {

    sealed interface Error : NetworkResponse {

        data class SocketTimeout(val exception: Exception) : Error

        data class IO(val exception: Exception) : Error

        data class Unknown(val exception: Exception) : Error

        data class Http(val statusCode: Int) : Error
    }

    data class Success(val statusCode: Int) : NetworkResponse
}
