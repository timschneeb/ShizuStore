/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.LabelChip
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.util.CommonUtil

internal fun detailTags(context: Context, details: AppDetails): List<String> = buildList {
    details.stars?.let {
        add(context.getString(R.string.app_stars, CommonUtil.formatCount(it.toLong())))
    }

    if (details.lastUpdated > 0L) add(CommonUtil.formatDate(details.lastUpdated))

    details.categories.firstOrNull()?.takeIf { it.isNotBlank() }?.let { add(it) }

    if (details.size > 0L) add(CommonUtil.addSiPrefix(details.size))

    if (details.minSdk > 0) add(context.getString(R.string.details_min_sdk, details.minSdk))

    details.license.takeIf { it.isNotBlank() }?.let { add(it) }
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
        items(items = tags, key = { it }) { tag ->
            LabelChip(
                text = tag,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = if (tag == category && onCategoryClick != null) {
                    Modifier.clickable(onClick = onCategoryClick)
                } else {
                    Modifier
                }
            )
        }
    }
}
