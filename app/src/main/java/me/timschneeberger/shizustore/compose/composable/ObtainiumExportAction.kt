/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.composable

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.data.helper.EXPORT_FILE_NAME
import me.timschneeberger.shizustore.data.helper.buildObtainiumExportJson
import me.timschneeberger.shizustore.data.helper.writeObtainiumExport
import me.timschneeberger.shizustore.data.room.entity.AppEntity

/**
 * Exports the fetched rows as an Obtainium config through the storage access
 * framework, then explains how to import the file. Obtainium cannot receive a
 * config through a share target, so it has to be picked up from storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ObtainiumExportButton(
    fetchApps: suspend () -> List<AppEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Saveable: the create-document dialog can outlive the composition, and
    // its result needs the exact document rendered before it opened.
    var pendingJson by rememberSaveable { mutableStateOf<String?>(null) }
    var showImportHint by rememberSaveable { mutableStateOf(false) }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingJson ?: return@rememberLauncherForActivityResult
        pendingJson = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            if (context.contentResolver.writeObtainiumExport(uri, json)) {
                showImportHint = true
            } else {
                Toast.makeText(
                    context,
                    R.string.obtainium_export_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below
        ),
        tooltip = { PlainTooltip { Text(stringResource(R.string.action_export_obtainium)) } },
        state = rememberTooltipState(),
        modifier = modifier
    ) {
        IconButton(
            onClick = {
                scope.launch {
                    val json = buildObtainiumExportJson(fetchApps())
                    if (json == null) {
                        Toast.makeText(
                            context,
                            R.string.obtainium_export_empty,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        pendingJson = json
                        createDocument.launch(EXPORT_FILE_NAME)
                    }
                }
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_obtainium),
                contentDescription = stringResource(R.string.action_export_obtainium)
            )
        }
    }

    if (showImportHint) {
        AlertDialog(
            onDismissRequest = { showImportHint = false },
            title = { Text(stringResource(R.string.obtainium_import_title)) },
            text = {
                Text(stringResource(R.string.obtainium_import_message, EXPORT_FILE_NAME))
            },
            confirmButton = {
                TextButton(onClick = { showImportHint = false }) {
                    Text(stringResource(R.string.action_close))
                }
            }
        )
    }
}
