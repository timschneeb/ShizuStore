/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * The search-in-app-bar treatment and the tag cloud follow the official F-Droid
 * client (GPL-3.0-or-later), app/src/main/kotlin/org/fdroid/ui/search/.
 */

package me.timschneeberger.shizustore.compose.ui.applist

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.categoryIcon
import me.timschneeberger.shizustore.compose.composable.AppRowSkeleton
import me.timschneeberger.shizustore.compose.composable.ContainedLoadingIndicator
import me.timschneeberger.shizustore.compose.composable.ExpressivePullToRefreshBox
import me.timschneeberger.shizustore.compose.composable.OfflineBanner
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.ScrollHint
import me.timschneeberger.shizustore.compose.composable.app.AppListItem
import me.timschneeberger.shizustore.compose.composable.rememberVisibleForAtLeast
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.data.model.AppListArgs
import me.timschneeberger.shizustore.data.model.AppSort
import me.timschneeberger.shizustore.data.model.CategoryTag
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.model.flatten
import me.timschneeberger.shizustore.data.sync.CatalogSyncFailure
import me.timschneeberger.shizustore.viewmodel.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    args: AppListArgs,
    modifier: Modifier = Modifier,
    searchHome: Boolean = false,
    showBack: Boolean = false,
    searchFocusRequest: Int = 0,
    onBack: () -> Unit = {},
    onNavigateTo: (Destination) -> Unit = {},
    viewModel: AppListViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { viewModel.initialize(args) }

    val currentArgs by viewModel.args.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val syncing by viewModel.syncing.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val syncFailure by viewModel.syncFailure.collectAsStateWithLifecycle()
    val atSearchHome by viewModel.atSearchHome.collectAsStateWithLifecycle()
    val useInstallCounts by viewModel.useInstallCountsForPopularity.collectAsStateWithLifecycle()
    val apps = viewModel.apps.collectAsLazyPagingItems()

    var searchExpanded by rememberSaveable { mutableStateOf(searchHome) }

    val searchFieldState = rememberTextFieldState(args.query)
    // Saveable (not plain remember): the list entry leaves composition while a
    // detail screen is on top, and only rememberSaveable survives the return.
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Guarded by appliedArgs: LaunchedEffect also fires when the screen re-enters
    // composition (back from details), which must keep position.
    var appliedArgs by remember { mutableStateOf<AppListArgs?>(null) }
    LaunchedEffect(currentArgs) {
        if (appliedArgs != null && appliedArgs != currentArgs) {
            listState.scrollToItem(0)
        }
        appliedArgs = currentArgs
    }

    LaunchedEffect(searchFocusRequest) {
        if (searchFocusRequest > 0) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Back from active search/list returns to the search home instead of closing the app.
    BackHandler(enabled = searchHome && !atSearchHome) {
        searchFieldState.setTextAndPlaceCursorAtEnd("")
        viewModel.clearAll()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    when {
                        showBack -> IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }

                        searchHome && !atSearchHome -> IconButton(
                            onClick = {
                                searchFieldState.setTextAndPlaceCursorAtEnd("")
                                viewModel.clearAll()
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    }
                },
                title = {
                    if (searchHome || searchExpanded) {
                        AppSearchField(
                            textFieldState = searchFieldState,
                            onQueryChange = viewModel::setQuery,
                            onSearchCleared = { viewModel.setQuery("") },
                            focusRequester = searchFocusRequester
                        )
                    } else {
                        Text(
                            text = listTitle(currentArgs, categories.orEmpty()),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    if (!searchHome && !searchExpanded) {
                        IconButton(onClick = { searchExpanded = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = stringResource(R.string.action_search)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (syncFailure != null) {
                OfflineBanner(onRetry = viewModel::retrySync)
            }

            if (!(searchHome && atSearchHome)) {
                AppListFilterChips(
                    args = currentArgs,
                    categories = categories.orEmpty(),
                    onCategory = viewModel::setCategory,
                    onPrice = viewModel::setPrice,
                    onSort = viewModel::setSort,
                    onRecommended = viewModel::setRecommended
                )
            }

            if (searchHome && atSearchHome) {
                SearchHome(
                    history = history,
                    categories = categories.orEmpty(),
                    onSearch = { query ->
                        searchFieldState.setTextAndPlaceCursorAtEnd(query)
                        viewModel.setQuery(query)
                    },
                    onClearHistory = viewModel::clearHistory,
                    onCategory = viewModel::setCategory
                )
            } else {
                ExpressivePullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = viewModel::retrySync,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppRows(
                        apps = apps,
                        listState = listState,
                        showStars = currentArgs.sort == AppSort.STARS,
                        showInstalls = currentArgs.sort == AppSort.DOWNLOADS && useInstallCounts,
                        syncing = syncing,
                        syncFailure = syncFailure,
                        onRetry = viewModel::retrySync,
                        onAppClick = { app ->
                            onNavigateTo(Destination.AppDetails(app.packageName))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
private fun AppSearchField(
    textFieldState: TextFieldState,
    onQueryChange: (String) -> Unit,
    onSearchCleared: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        snapshotFlow { textFieldState.text.toString() }
            // Skip the initial empty value; otherwise the debounce fires
            // setQuery("") on arrival and kicks the search home to the list.
            .drop(1)
            .distinctUntilChanged()
            .debounce(SEARCH_DEBOUNCE_MS)
            .collectLatest { onQueryChange(it) }
    }

    SearchBarDefaults.InputField(
        textFieldState = textFieldState,
        searchBarState = rememberSearchBarState(),
        onSearch = { },
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = null
            )
        },
        trailingIcon = {
            if (textFieldState.text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        textFieldState.setTextAndPlaceCursorAtEnd("")
                        onSearchCleared()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_clear),
                        contentDescription = stringResource(R.string.action_clear)
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth().focusRequester(focusRequester)
    )
}

@Composable
private fun SearchHome(
    history: List<String>,
    categories: List<CategoryTag>,
    onSearch: (String) -> Unit,
    onClearHistory: () -> Unit,
    onCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.spacing_large))
    ) {
        if (history.isNotEmpty()) {
            item(key = "historyHeader") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = dimensionResource(R.dimen.spacing_large))
                ) {
                    Text(
                        text = stringResource(R.string.search_history),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(stringResource(R.string.action_clear))
                    }
                }
            }
            items(history, key = { it }) { query ->
                ListItem(
                    leadingContent = {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { onSearch(query) }
                ) {
                    Text(query)
                }
            }
        }

        item(key = "categories") {
            Column(
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing_large))
            ) {
                Text(
                    text = stringResource(R.string.title_categories),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = dimensionResource(R.dimen.spacing_small))
                )
                CategoryTagCloud(categories = categories, onCategory = onCategory)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryTagCloud(
    categories: List<CategoryTag>,
    onCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontal = dimensionResource(R.dimen.spacing_small)
    val vertical = dimensionResource(R.dimen.spacing_xsmall)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(horizontal),
        verticalArrangement = Arrangement.spacedBy(vertical)
    ) {
        // Null clears the category filter, so "All" lands on the unfiltered list.
        AssistChip(
            onClick = { onCategory(null) },
            label = { Text(stringResource(R.string.filter_all)) },
            leadingIcon = { CategoryTagIcon(slug = "") },
            modifier = Modifier.height(dimensionResource(R.dimen.chip_height))
        )
        categories.forEach { tag -> CategoryTagChip(tag = tag, onCategory = onCategory) }
    }
}

@Composable
private fun CategoryTagChip(
    tag: CategoryTag,
    onCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!tag.isContainer) {
        AssistChip(
            onClick = { onCategory(tag.slug) },
            label = { Text(tag.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { CategoryTagIcon(tag.slug) },
            modifier = modifier.height(dimensionResource(R.dimen.chip_height))
        )
        return
    }

    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(tag.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { CategoryTagIcon(tag.slug) },
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_down),
                    contentDescription = null
                )
            },
            modifier = Modifier.height(dimensionResource(R.dimen.chip_height))
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            tag.children.forEach { child ->
                DropdownMenuItem(
                    text = { Text(child.name) },
                    leadingIcon = { CategoryTagIcon(child.slug) },
                    onClick = {
                        expanded = false
                        onCategory(child.slug)
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoryTagIcon(slug: String) {
    Icon(
        imageVector = categoryIcon(slug),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(dimensionResource(R.dimen.icon_size_chip))
    )
}

@Composable
private fun AppRows(
    apps: LazyPagingItems<ResolvedApp>,
    listState: LazyListState,
    showStars: Boolean,
    showInstalls: Boolean,
    syncing: Boolean,
    syncFailure: CatalogSyncFailure?,
    onRetry: () -> Unit,
    onAppClick: (ResolvedApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val isInitialLoad = apps.loadState.refresh is LoadState.Loading && apps.itemCount == 0
    val isRefreshing = apps.loadState.refresh is LoadState.Loading && apps.itemCount > 0
    val showRefreshing = rememberVisibleForAtLeast(isRefreshing, MIN_LIST_LOADING_MS)
    val isEmpty = apps.loadState.refresh is LoadState.NotLoading && apps.itemCount == 0

    val listPadding = PaddingValues(bottom = dimensionResource(R.dimen.spacing_large))

    Box(modifier = modifier.fillMaxSize()) {
        when {
            isInitialLoad -> AppRowSkeleton(contentPadding = listPadding)

            showRefreshing -> ContainedLoadingIndicator()

            isEmpty -> Placeholder(
                painter = painterResource(R.drawable.ic_search),
                message = if (syncFailure != null) {
                    stringResource(R.string.apps_sync_failed)
                } else {
                    stringResource(R.string.search_no_results)
                },
                detail = if (syncFailure != null) {
                    null
                } else {
                    stringResource(
                        if (syncing) R.string.apps_empty_syncing else R.string.apps_empty_detail
                    )
                },
                inProgress = syncing && syncFailure == null,
                actionLabel = if (syncFailure != null) {
                    stringResource(R.string.action_retry)
                } else {
                    null
                },
                onAction = if (syncFailure != null) onRetry else null
            )

            else -> Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = listPadding
                ) {
                    items(
                        count = apps.itemCount,
                        key = apps.itemKey { it.slug }
                    ) { index ->
                        apps[index]?.let { app ->
                            AppListItem(
                                app = app,
                                onClick = { onAppClick(app) },
                                showStars = showStars,
                                showInstalls = showInstalls
                            )
                        }
                    }
                }

                ScrollHint(
                    listState = listState,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

private fun sortLabel(sort: AppSort): Int = when (sort) {
    AppSort.NAME -> R.string.search_sort_name
    AppSort.RECENTLY_ADDED -> R.string.apps_recently_added
    AppSort.RECENTLY_UPDATED -> R.string.apps_recently_updated
    AppSort.STARS -> R.string.search_sort_stars
    AppSort.DOWNLOADS -> R.string.search_sort_popularity
    AppSort.SIZE_DESC -> R.string.search_sort_size
}

@Composable
private fun listTitle(args: AppListArgs, categories: List<CategoryTag>): String {
    args.categorySlug?.let { slug ->
        return categories.flatten().firstOrNull { it.slug == slug }?.name ?: slug
    }
    return when {
        args.recommended -> stringResource(R.string.apps_recommended)
        args.sort != AppSort.NAME -> stringResource(sortLabel(args.sort))
        else -> stringResource(R.string.title_apps)
    }
}

private const val SEARCH_DEBOUNCE_MS = 250L

private const val MIN_LIST_LOADING_MS = 100L
