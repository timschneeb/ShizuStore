/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.applist

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoneyOff
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.SortByAlpha
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.categoryIcon
import me.timschneeberger.shizustore.compose.composable.AuroraListItem
import me.timschneeberger.shizustore.compose.composable.ItemSelection
import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.data.model.AppPrice
import me.timschneeberger.shizustore.data.model.AppSort
import me.timschneeberger.shizustore.data.model.CategoryTag
import me.timschneeberger.shizustore.data.model.flatten

@Composable
fun AppListFilterChips(
    args: AppListArgs,
    categories: List<CategoryTag>,
    onCategory: (String?) -> Unit,
    onPrice: (AppPrice?) -> Unit,
    onSort: (AppSort) -> Unit,
    onRecommended: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var openSheet by rememberSaveable { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val flatCategories = remember(categories) { categories.flatten() }

    val gap = dimensionResource(R.dimen.spacing_small)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(R.dimen.spacing_large), vertical = gap),
        horizontalArrangement = Arrangement.spacedBy(gap)
    ) {
        val categoryName = args.categorySlug
            ?.let { slug -> flatCategories.firstOrNull { it.slug == slug }?.name }
            ?: stringResource(R.string.filter_all)
        FilterChip(
            selected = args.categorySlug != null,
            onClick = { openSheet = SHEET_CATEGORY },
            label = { Text(categoryName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = {
                Icon(
                    imageVector = args.categorySlug?.let(::categoryIcon) ?: Icons.Rounded.Category,
                    contentDescription = null
                )
            }
        )

        FilterChip(
            selected = args.sort != AppSort.NAME,
            onClick = { openSheet = SHEET_SORT },
            label = { Text(stringResource(sortLabel(args.sort)), maxLines = 1) },
            leadingIcon = { Icon(imageVector = sortIcon(args.sort), contentDescription = null) }
        )

        FilterChip(
            selected = args.recommended,
            onClick = { onRecommended(!args.recommended) },
            label = { Text(stringResource(R.string.apps_recommended), maxLines = 1) },
            leadingIcon = {
                Icon(imageVector = Icons.Rounded.AutoAwesome, contentDescription = null)
            }
        )

        FilterChip(
            selected = args.price != null,
            onClick = { openSheet = SHEET_PRICE },
            label = { Text(stringResource(priceChipLabel(args.price)), maxLines = 1) },
            leadingIcon = { Icon(imageVector = priceIcon(args.price), contentDescription = null) }
        )
    }

    when (openSheet) {
        SHEET_CATEGORY -> FilterSheet(
            title = stringResource(R.string.filter_title_category),
            options = remember(context, categories) { categoryOptions(context, categories) },
            selectedId = args.categorySlug,
            onSelect = {
                onCategory(it)
                openSheet = null
            },
            onDismiss = { openSheet = null }
        )

        SHEET_PRICE -> FilterSheet(
            title = stringResource(R.string.filter_title_price),
            options = remember(context) { priceOptions(context) },
            selectedId = args.price?.name,
            onSelect = { id ->
                onPrice(id?.let(AppPrice::valueOf))
                openSheet = null
            },
            onDismiss = { openSheet = null }
        )

        SHEET_SORT -> FilterSheet(
            title = stringResource(R.string.filter_title_sort),
            options = remember(context) { sortOptions(context) },
            selectedId = args.sort.name,
            onSelect = { id ->
                id?.let { onSort(AppSort.valueOf(it)) }
                openSheet = null
            },
            onDismiss = { openSheet = null }
        )
    }
}

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    title: String,
    options: List<FilterOption>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        // The sheet is a fresh composition on every open, so bring the
        // current selection into view instead of always starting at the top.
        val listState = rememberLazyListState()
        val selectedIndex = options.indexOfFirst { !it.isHeader && it.id == selectedId }
        LaunchedEffect(selectedIndex) {
            if (selectedIndex > 0) listState.scrollToItem(selectedIndex)
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
            items(options) { option ->
                if (option.isHeader) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    val selected = option.id == selectedId
                    AuroraListItem(
                        headline = option.label,
                        selection = ItemSelection.Radio(selected),
                        onClick = { onSelect(option.id) },
                        leading = if (option.icon != null) {
                            { Icon(imageVector = option.icon, contentDescription = null) }
                        } else {
                            null
                        },
                        trailing = if (selected) {
                            { Icon(imageVector = Icons.Rounded.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        modifier = if (option.indented) {
                            Modifier.padding(start = 24.dp)
                        } else {
                            Modifier
                        }
                    )
                }
            }
        }
    }
}

private fun categoryOptions(context: Context, categories: List<CategoryTag>): List<FilterOption> =
    buildList {
        add(
            FilterOption(
                id = null,
                label = context.getString(R.string.filter_all),
                icon = Icons.Rounded.Category
            )
        )
        categories.forEach { root ->
            if (root.isContainer) {
                add(FilterOption(id = null, label = root.name, isHeader = true))
                root.children.forEach { child ->
                    add(
                        FilterOption(
                            id = child.slug,
                            label = child.name,
                            icon = categoryIcon(child.slug),
                            indented = true
                        )
                    )
                }
            } else {
                add(FilterOption(id = root.slug, label = root.name, icon = categoryIcon(root.slug)))
            }
        }
    }

private fun priceOptions(context: Context): List<FilterOption> = listOf(
    FilterOption(
        id = null,
        label = context.getString(R.string.filter_all),
        icon = Icons.Rounded.AttachMoney
    ),
    FilterOption(
        id = AppPrice.FREE.name,
        label = context.getString(R.string.filter_free),
        icon = Icons.Rounded.MoneyOff
    ),
    FilterOption(
        id = AppPrice.IAP.name,
        label = context.getString(R.string.filter_iap),
        icon = Icons.Rounded.Redeem
    ),
    FilterOption(
        id = AppPrice.IAP_OR_PAID.name,
        label = context.getString(R.string.filter_iap_or_paid),
        icon = Icons.Rounded.Paid
    )
)

private fun sortOptions(context: Context): List<FilterOption> = listOf(
    FilterOption(
        id = AppSort.NAME.name,
        label = context.getString(R.string.search_sort_name),
        icon = Icons.Rounded.SortByAlpha
    ),
    FilterOption(
        id = AppSort.RECENTLY_ADDED.name,
        label = context.getString(R.string.apps_recently_added),
        icon = Icons.Rounded.Schedule
    ),
    FilterOption(
        id = AppSort.RECENTLY_UPDATED.name,
        label = context.getString(R.string.apps_recently_updated),
        icon = Icons.Rounded.Update
    ),
    FilterOption(
        id = AppSort.STARS.name,
        label = context.getString(R.string.search_sort_stars),
        icon = Icons.Rounded.Star
    ),
    FilterOption(
        id = AppSort.DOWNLOADS.name,
        label = context.getString(R.string.search_sort_popularity),
        icon = Icons.Rounded.Download
    ),
    FilterOption(
        id = AppSort.SIZE_DESC.name,
        label = context.getString(R.string.search_sort_size),
        icon = Icons.Rounded.SdStorage
    )
)

private fun priceChipLabel(price: AppPrice?): Int = when (price) {
    null -> R.string.filter_price
    AppPrice.FREE -> R.string.filter_free
    AppPrice.IAP -> R.string.filter_iap_short
    AppPrice.IAP_OR_PAID -> R.string.filter_iap_or_paid_short
}

private fun priceIcon(price: AppPrice?): ImageVector = when (price) {
    null -> Icons.Rounded.AttachMoney
    AppPrice.FREE -> Icons.Rounded.MoneyOff
    AppPrice.IAP -> Icons.Rounded.Redeem
    AppPrice.IAP_OR_PAID -> Icons.Rounded.Paid
}

internal fun sortLabel(sort: AppSort): Int = when (sort) {
    AppSort.NAME -> R.string.search_sort_name
    AppSort.RECENTLY_ADDED -> R.string.apps_recently_added
    AppSort.RECENTLY_UPDATED -> R.string.apps_recently_updated
    AppSort.STARS -> R.string.search_sort_stars
    AppSort.DOWNLOADS -> R.string.search_sort_popularity
    AppSort.SIZE_DESC -> R.string.search_sort_size
}

private fun sortIcon(sort: AppSort): ImageVector = when (sort) {
    AppSort.NAME -> Icons.Rounded.SortByAlpha
    AppSort.RECENTLY_ADDED -> Icons.Rounded.Schedule
    AppSort.RECENTLY_UPDATED -> Icons.Rounded.Update
    AppSort.STARS -> Icons.Rounded.Star
    AppSort.DOWNLOADS -> Icons.Rounded.Download
    AppSort.SIZE_DESC -> Icons.Rounded.SdStorage
}

private data class FilterOption(
    val id: String?,
    val label: String,
    val icon: ImageVector? = null,
    val isHeader: Boolean = false,
    val indented: Boolean = false
)

private const val SHEET_CATEGORY = "category"
private const val SHEET_PRICE = "price"
private const val SHEET_SORT = "sort"
