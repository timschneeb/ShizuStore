/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.model

import androidx.room.Ignore
import me.timschneeberger.shizustore.data.api.Availability

data class AppDetails(
    val packageName: String,
    val repoId: Int,
    val repoName: String,
    val repoAddress: String,
    val name: String,
    val summary: String,
    val description: String,
    val iconUrl: String?,
    val license: String,
    val authorName: String?,
    val authorEmail: String?,
    val authorPhone: String?,
    val authorWebSite: String?,
    val webSite: String?,
    val sourceCode: String?,
    val issueTracker: String?,
    val changelog: String?,
    val translation: String?,
    val donate: List<String>,
    val liberapay: String?,
    val openCollective: String?,
    val bitcoin: String?,
    val litecoin: String?,
    val categories: List<String>,
    val antiFeatures: List<String>,
    val screenshots: List<String>,
    val added: Long,
    val lastUpdated: Long,
    val versionCode: Long,
    val versionName: String,
    val signer: String?,
    val size: Long,
    val minSdk: Int,
    val whatsNew: String,
    val permissions: List<String>,
    val installedVersionCode: Long?,
    val installedSigner: String?,
    @Ignore val slug: String = "",
    val iconHash: String? = null,
    @Ignore val availability: Availability = Availability.DIRECT_APK,
    val storeUrl: String? = null,
    val url: String? = null,
    val sourceUrl: String? = null,
    @Ignore val iconAdaptive: Boolean = false,
    val hasPaid: Boolean = false,
    val hasIap: Boolean = false,
    val stars: Int? = null
)
