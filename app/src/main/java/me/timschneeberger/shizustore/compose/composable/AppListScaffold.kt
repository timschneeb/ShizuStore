/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.dimensionResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.ContentPhase
import me.timschneeberger.shizustore.compose.composable.app.AppListItem
import me.timschneeberger.shizustore.data.model.ResolvedApp

@Composable
fun AppListScaffold(
    title: String,
    contentPhase: ContentPhase,
    listState: LazyListState,
    emptyPainter: Painter,
    emptyMessage: String,
    onNavigateBack: () -> Unit,
    transitionLabel: String,
    modifier: Modifier = Modifier,
    emptyDetail: String? = null,
    showScrollHint: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val listPadding = PaddingValues(bottom = dimensionResource(R.dimen.spacing_large))

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = title,
                onNavigateBack = onNavigateBack
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = contentPhase,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = transitionLabel
            ) { phase ->
                when (phase) {
                    ContentPhase.Loading -> AppRowSkeleton(contentPadding = listPadding)

                    ContentPhase.Empty -> Placeholder(
                        painter = emptyPainter,
                        message = emptyMessage,
                        detail = emptyDetail
                    )

                    ContentPhase.Loaded -> if (showScrollHint) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = listPadding,
                                content = content
                            )

                            ScrollHint(
                                listState = listState,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = listPadding,
                            content = content
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemovableAppItem(
    app: ResolvedApp,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    icon: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    supporting: String? = app.authorName?.takeIf { it.isNotBlank() } ?: app.summary
) {
    RemovableListItem(onRemove = onRemove, modifier = modifier) { triggerRemove ->
        AppListItem(
            app = app,
            onClick = onClick,
            supporting = supporting,
            trailing = {
                IconButton(onClick = triggerRemove) {
                    Icon(
                        painter = icon,
                        contentDescription = contentDescription
                    )
                }
            }
        )
    }
}
