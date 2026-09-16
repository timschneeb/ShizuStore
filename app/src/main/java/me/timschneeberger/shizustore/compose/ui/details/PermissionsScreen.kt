/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.Info
import me.timschneeberger.shizustore.compose.composable.LoadingIndicatorBox
import me.timschneeberger.shizustore.compose.composable.Placeholder
import me.timschneeberger.shizustore.compose.composable.TopAppBar
import me.timschneeberger.shizustore.compose.navigation.Destination
import me.timschneeberger.shizustore.viewmodel.AppDetailsUiState
import me.timschneeberger.shizustore.viewmodel.AppDetailsViewModel

@Composable
fun PermissionsScreen(
    packageName: String,
    modifier: Modifier = Modifier,
    viewModel: AppDetailsViewModel = hiltViewModel(),
    onNavigateTo: (Destination) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(packageName) { viewModel.load(packageName) }

    val declared = (uiState as? AppDetailsUiState.Loaded)?.details?.permissions

    val groups = remember(declared) {
        declared
            ?.map { resolvePermission(context, it) }
            ?.sortedBy { it.label.lowercase() }
            ?.groupBy { it.category }
            ?.toSortedMap(compareBy { it.ordinal })
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = stringResource(R.string.details_permissions),
                onNavigateBack = { onNavigateTo(Destination.Back) }
            )
        }
    ) { padding ->
        when {
            groups == null -> LoadingIndicatorBox(
                modifier = Modifier.padding(padding)
            )

            groups.isEmpty() -> Placeholder(
                modifier = Modifier.padding(padding),
                painter = painterResource(R.drawable.ic_shield),
                message = stringResource(R.string.details_permissions_none)
            )

            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_xsmall)
                )
            ) {
                groups.forEach { (category, entries) ->
                    item(key = "header-${category.name}") {
                        PermissionCategoryHeader(
                            title = stringResource(
                                when (category) {
                                    PermissionCategory.DANGEROUS ->
                                        R.string.details_permissions_dangerous

                                    PermissionCategory.KNOWN -> R.string.details_permissions_known
                                    PermissionCategory.CUSTOM -> R.string.details_permissions_custom
                                }
                            )
                        )
                    }
                    items(items = entries, key = { it.description ?: it.label }) { entry ->
                        val icon = remember(entry.icon) { entry.icon?.let(::BitmapPainter) }
                        Info(
                            title = entry.label,
                            description = entry.description,
                            // No generic fallback icon: rows without a permission
                            // or group icon keep blank space so rows stay aligned.
                            painter = icon ?: remember { ColorPainter(Color.Transparent) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Category headings read as labels, not row titles, so they do not blend into
 * the permission names below them.
 */
@Composable
private fun PermissionCategoryHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.spacing_large),
                end = dimensionResource(R.dimen.spacing_large),
                top = dimensionResource(R.dimen.spacing_large),
                bottom = dimensionResource(R.dimen.spacing_xsmall)
            )
    )
}
