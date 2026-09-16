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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.LabelChip
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
}

internal fun detailTags(context: Context, details: AppDetails): List<DetailTag> = buildList {
    details.stars?.let {
        add(
            DetailTag.Text(
                context.getString(R.string.app_stars, CommonUtil.formatCount(it.toLong()))
            )
        )
    }

    if (details.lastUpdated > 0L) add(DetailTag.Text(CommonUtil.formatDate(details.lastUpdated)))

    details.categories.firstOrNull()?.takeIf { it.isNotBlank() }?.let { add(DetailTag.Text(it)) }

    if (details.size > 0L) add(DetailTag.Text(CommonUtil.addSiPrefix(details.size)))

    if (details.minSdk > 0) add(DetailTag.MinSdk(details.minSdk))

    details.license.takeIf { it.isNotBlank() }?.let { add(DetailTag.Text(it)) }
}

@Composable
fun DetailsTags(
    details: AppDetails,
    modifier: Modifier = Modifier,
    onCategoryClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val tags = detailTags(context, details)
    val category = details.categories.firstOrNull()?.takeIf { it.isNotBlank() }

    if (tags.isEmpty()) return

    LazyRow(
        modifier = modifier.padding(vertical = dimensionResource(R.dimen.spacing_small)),
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.spacing_large)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        items(items = tags, key = { it.key }) { tag ->
            val isCategory = tag is DetailTag.Text && tag.value == category
            LabelChip(
                text = when (tag) {
                    is DetailTag.Text -> tag.value
                    is DetailTag.MinSdk -> minSdkLabel(context, tag.apiLevel)
                },
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = if (isCategory && onCategoryClick != null) {
                    Modifier.clickable(onClick = onCategoryClick)
                } else {
                    Modifier
                },
                leadingIcon = if (tag is DetailTag.MinSdk) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_android_head),
                            contentDescription = null,
                            modifier = Modifier.size(dimensionResource(R.dimen.icon_size_chip))
                        )
                    }
                } else {
                    null
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
