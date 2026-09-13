/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.data.room

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import me.timschneeberger.shizustore.data.api.AppType
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.api.CategorySection
import me.timschneeberger.shizustore.data.api.Listing
import me.timschneeberger.shizustore.data.api.ShizuJson
import me.timschneeberger.shizustore.data.api.SourceKind
import me.timschneeberger.shizustore.data.model.DownloadFailure
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.room.entity.CategoryPath

class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        runCatching { Json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())

    @TypeConverter
    fun fromCategoryPathList(value: List<CategoryPath>): String = ShizuJson.encodeToString(value)

    @TypeConverter
    fun toCategoryPathList(value: String): List<CategoryPath> = runCatching {
        ShizuJson.decodeFromString<List<CategoryPath>>(value)
    }.getOrDefault(emptyList())

    @TypeConverter
    fun fromAvailability(value: Availability): String = value.wire

    @TypeConverter
    fun toAvailability(value: String): Availability =
        Availability.fromWire(value) ?: Availability.LINK_ONLY

    @TypeConverter
    fun fromListing(value: Listing): String = value.wire

    @TypeConverter
    fun toListing(value: String): Listing = Listing.fromWire(value) ?: Listing.MAIN

    @TypeConverter
    fun fromAppType(value: AppType): String = value.wire

    @TypeConverter
    fun toAppType(value: String): AppType = AppType.fromWire(value) ?: AppType.APP

    @TypeConverter
    fun fromCategorySection(value: CategorySection): String = value.wire

    @TypeConverter
    fun toCategorySection(value: String): CategorySection =
        CategorySection.fromWire(value) ?: CategorySection.APPS

    @TypeConverter
    fun fromSourceKind(value: SourceKind): String = value.wire

    @TypeConverter
    fun toSourceKind(value: String): SourceKind = SourceKind.fromWire(value) ?: SourceKind.OTHER

    @TypeConverter
    fun fromDownloadStatus(value: DownloadStatus): String = value.name

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadStatus =
        runCatching { DownloadStatus.valueOf(value) }.getOrDefault(DownloadStatus.FAILED)

    @TypeConverter
    fun fromDownloadFailure(value: DownloadFailure?): String? = value?.name

    @TypeConverter
    fun toDownloadFailure(value: String?): DownloadFailure? =
        value?.let { runCatching { DownloadFailure.valueOf(it) }.getOrNull() }
}
