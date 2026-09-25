/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.categoryIcon
import me.timschneeberger.shizustore.compose.composable.LabelChip
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.util.AndroidVersion
import me.timschneeberger.shizustore.util.CommonUtil

internal sealed interface DetailTag {
    val key: String

    data class Text(val value: String) : DetailTag {
        override val key: String get() = "text:$value"
    }

    data class MinSdk(val apiLevel: Int) : DetailTag {
        override val key: String get() = "minSdk:$apiLevel"
    }

    /** Age of the recorded version, shown before the license chip. */
    data class Updated(val epochMillis: Long) : DetailTag {
        override val key: String get() = "updated:$epochMillis"
    }

    /** Star count for entries without a stats strip, shown first. */
    data class Stars(val count: Int) : DetailTag {
        override val key: String get() = "stars:$count"
    }

    /** Declared Dhizuku permission; the app can run under Dhizuku instead of Shizuku. */
    data object Dhizuku : DetailTag {
        override val key: String get() = "dhizuku"
    }
}

internal fun detailTags(details: AppDetails): List<DetailTag> = buildList {
    // Non-installable entries have no stats strip, so their stars ride the
    // chip row instead.
    if (details.availability != Availability.DIRECT_APK) {
        details.stars?.takeIf { it > 0 }?.let { add(DetailTag.Stars(it)) }
    }

    details.categories.firstOrNull()?.takeIf { it.isNotBlank() }?.let { add(DetailTag.Text(it)) }

    if (details.minSdk > 0) add(DetailTag.MinSdk(details.minSdk))

    details.versionUpdatedAtMillis?.let { add(DetailTag.Updated(it)) }

    details.license.takeIf { it.isNotBlank() }?.let { add(DetailTag.Text(it)) }

    if (details.dhizukuDeclared) add(DetailTag.Dhizuku)
}

@Composable
fun DetailsTags(
    details: AppDetails,
    categorySlug: String? = null,
    modifier: Modifier = Modifier,
    onCategoryClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val tags = remember(details) { detailTags(details) }
    val category = details.categories.firstOrNull()?.takeIf { it.isNotBlank() }
    // Unknown slugs fall back to the generic icon, same as the list filters.
    val categoryIconRes = categorySlug?.takeIf { it.isNotBlank() }
        ?.let(::categoryIcon) ?: R.drawable.ic_category

    if (tags.isEmpty()) return

    LazyRow(
        modifier = modifier.padding(
            top = dimensionResource(R.dimen.spacing_xsmall),
            bottom = dimensionResource(R.dimen.spacing_small)
        ),
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        items(items = tags, key = { it.key }) { tag ->
            val isCategory = tag is DetailTag.Text && tag.value == category
            val iconRes = when {
                tag is DetailTag.MinSdk -> R.drawable.ic_android_head
                isCategory -> categoryIconRes
                tag is DetailTag.Updated -> R.drawable.ic_updates
                tag is DetailTag.Stars -> R.drawable.ic_star
                else -> null
            }
            LabelChip(
                text = when (tag) {
                    is DetailTag.Text -> tag.value
                    is DetailTag.MinSdk -> minSdkLabel(context, tag.apiLevel)
                    is DetailTag.Updated -> CommonUtil.relativeAge(context, tag.epochMillis)
                    is DetailTag.Stars -> CommonUtil.formatCount(tag.count.toLong())
                    DetailTag.Dhizuku -> stringResource(R.string.details_dhizuku)
                },
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = if (isCategory && onCategoryClick != null) {
                    Modifier.clickable(onClick = onCategoryClick)
                } else {
                    Modifier
                },
                leadingIcon = iconRes?.let { res ->
                    {
                        Icon(
                            painter = painterResource(res),
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_chip))
                        )
                    }
                }
            )
        }
    }
}

private fun minSdkLabel(context: Context, apiLevel: Int): String {
    val version = AndroidVersion.name(apiLevel)
    return if (version != null) {
        context.getString(R.string.details_min_android, version)
    } else {
        context.getString(R.string.details_min_sdk, apiLevel)
    }
}
