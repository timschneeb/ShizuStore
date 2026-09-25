/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.helper.SourceLauncher
import me.timschneeberger.shizustore.data.room.entity.TrackerTag

/**
 * Exodus trackers the server matched in the primary APK's code signatures.
 * Matching is signature based, so an empty list is not proof the app is
 * tracker-free; the note says so instead of implying a clean result. Tapping
 * the card opens the per-tracker tag breakdown, where tapping a tracker
 * searches the web for it.
 */
@Composable
fun TrackersNotice(
    trackers: List<String>,
    trackerTags: List<TrackerTag>,
    modifier: Modifier = Modifier
) {
    if (trackers.isEmpty()) return

    var showDialog by rememberSaveable { mutableStateOf(false) }
    // The server groups tags per tracker; trackers without tags keep an empty
    // subtitle rather than disappearing from the dialog.
    val entries = remember(trackers, trackerTags) {
        val tagsByName = trackerTags.associate { it.name to it.tags }
        trackers.map { TrackerTag(it, tagsByName[it].orEmpty()) }
    }
    // A single tracker shows its tags inline; several keep the card to names and
    // defer the tags to the dialog.
    val cardText = entries.singleOrNull()?.let { tracker ->
        if (tracker.tags.isEmpty()) {
            tracker.name
        } else {
            "${tracker.name} (${tracker.tags.joinToString(", ")})"
        }
    } ?: trackers.joinToString(", ")

    Surface(
        onClick = { showDialog = true },
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.spacing_large),
                top = dimensionResource(R.dimen.spacing_xsmall),
                end = dimensionResource(R.dimen.spacing_large),
                bottom = dimensionResource(R.dimen.spacing_small)
            )
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
            modifier = Modifier.padding(
                horizontal = dimensionResource(R.dimen.spacing_large),
                vertical = dimensionResource(R.dimen.spacing_medium)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_fingerprint),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.details_trackers_title, trackers.size),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = cardText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }

    if (showDialog) {
        TrackersDialog(entries, onDismiss = { showDialog = false })
    }
}

/** One item per tracker: name on top, its Exodus category tags as the subtitle. */
@Composable
private fun TrackersDialog(trackers: List<TrackerTag>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.details_trackers_title, trackers.size)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                trackers.forEach { tracker ->
                    Surface(
                        onClick = {
                            SourceLauncher.open(context, trackerSearchUrl(tracker.name))
                        },
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.radius_medium)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = dimensionResource(R.dimen.spacing_medium),
                                vertical = dimensionResource(R.dimen.spacing_small)
                            )
                        ) {
                            Text(
                                text = tracker.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            if (tracker.tags.isNotEmpty()) {
                                Text(
                                    text = tracker.tags.joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.details_trackers_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_xsmall))
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

/** Look the tracker up on the web; the dialog has no per-tracker detail page. */
private fun trackerSearchUrl(name: String): String =
    "https://www.google.com/search?q=" + Uri.encode(name)
