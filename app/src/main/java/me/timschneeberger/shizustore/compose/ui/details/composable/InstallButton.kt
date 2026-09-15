/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.app.ProgressBar
import me.timschneeberger.shizustore.compose.composable.app.barFor
import me.timschneeberger.shizustore.compose.composable.app.statusCaption
import me.timschneeberger.shizustore.compose.stringRes
import me.timschneeberger.shizustore.data.api.Availability
import me.timschneeberger.shizustore.data.model.DownloadStatus
import me.timschneeberger.shizustore.data.model.InstallDispatch
import me.timschneeberger.shizustore.data.model.Installer
import me.timschneeberger.shizustore.data.model.ResolvedApp
import me.timschneeberger.shizustore.data.room.entity.Download

internal enum class InstallAction {
    INSTALL,
    CANCEL,
    OPEN,
    UNINSTALL,
    OPEN_STORE,
    OPEN_LINK
}

internal data class ActionButton(
    val label: String,
    val action: InstallAction,
    val enabled: Boolean = true
)

internal data class InstallButtonState(
    val primary: ActionButton,
    val secondary: ActionButton?,
    val caption: String?,
    val captionIsError: Boolean = false,
    val bar: ProgressBar
)

internal fun installButtonState(
    context: Context,
    app: ResolvedApp,
    download: Download?,
    canOpen: Boolean
): InstallButtonState {
    val idle = idleButtonState(context, app, canOpen)

    val isForResolvedVersion = download != null && download.versionCode == app.versionCode
    val signerBlocked = app.signerDiffersFromInstalled

    val uninstall = if (app.isInstalled) {
        ActionButton(context.getString(R.string.action_uninstall), InstallAction.UNINSTALL)
    } else {
        null
    }

    return when (download?.status) {
        null -> idle

        DownloadStatus.QUEUED,
        DownloadStatus.DOWNLOADING,
        DownloadStatus.VERIFYING -> inFlight(
            context = context,
            canCancel = true,
            caption = statusCaption(context, download.status, download.progress),
            bar = if (download.status == DownloadStatus.VERIFYING) {
                ProgressBar.Indeterminate
            } else {
                barFor(download.progress)
            }
        )

        DownloadStatus.INSTALLING -> inFlight(
            context = context,
            canCancel = false,
            caption = statusCaption(context, download.status, download.progress),
            // No install-phase progress exists (the fraction is the stale
            // download value), so spin instead of showing a stuck ring.
            bar = ProgressBar.Indeterminate
        )

        DownloadStatus.AWAITING_CONFIRMATION -> inFlight(
            context = context,
            canCancel = false,
            caption = statusCaption(context, download.status),
            bar = ProgressBar.Indeterminate
        )

        DownloadStatus.INSTALLED -> if (isForResolvedVersion && app.isInstalled) {
            openButtonState(context, canOpen)
        } else {
            idle
        }

        DownloadStatus.COMPLETED -> if (isForResolvedVersion && !signerBlocked) {
            InstallButtonState(
                primary = ActionButton(
                    context.getString(R.string.action_install),
                    InstallAction.INSTALL
                ),
                secondary = uninstall,
                caption = statusCaption(context, download.status),
                bar = ProgressBar.None
            )
        } else {
            idle
        }

        DownloadStatus.FAILED -> if (signerBlocked) {
            idle
        } else {
            InstallButtonState(
                primary = ActionButton(
                    context.getString(R.string.action_retry),
                    InstallAction.INSTALL
                ),
                secondary = uninstall,
                caption = download.error
                    ?.let { context.getString(it.stringRes()) }
                    ?: statusCaption(context, download.status),
                captionIsError = true,
                bar = ProgressBar.None
            )
        }

        DownloadStatus.CANCELLED,
        DownloadStatus.UNAVAILABLE -> idle
    }
}

private fun inFlight(context: Context, canCancel: Boolean, caption: String, bar: ProgressBar) =
    InstallButtonState(
        primary = ActionButton(
            context.getString(R.string.action_open),
            InstallAction.OPEN,
            enabled = false
        ),
        secondary = ActionButton(
            context.getString(R.string.action_cancel),
            InstallAction.CANCEL,
            enabled = canCancel
        ),
        caption = caption,
        bar = bar
    )

private fun idleButtonState(
    context: Context,
    app: ResolvedApp,
    canOpen: Boolean
): InstallButtonState = when {
    app.availability == Availability.LINK_ONLY -> InstallButtonState(
        primary = ActionButton(
            context.getString(R.string.action_open_link),
            InstallAction.OPEN_LINK
        ),
        secondary = null,
        caption = null,
        bar = ProgressBar.None
    )

    app.availability == Availability.PLAY_REDIRECT -> InstallButtonState(
        primary = ActionButton(
            context.getString(R.string.action_open_play),
            InstallAction.OPEN_STORE
        ),
        secondary = null,
        caption = null,
        bar = ProgressBar.None
    )

    app.hasUpdate -> InstallButtonState(
        primary = ActionButton(
            context.getString(R.string.action_update),
            InstallAction.INSTALL
        ),
        secondary = ActionButton(
            context.getString(R.string.action_uninstall),
            InstallAction.UNINSTALL
        ),
        caption = null,
        bar = ProgressBar.None
    )

    app.isInstalled -> openButtonState(context, canOpen)

    else -> InstallButtonState(
        primary = ActionButton(
            context.getString(R.string.action_install),
            InstallAction.INSTALL
        ),
        secondary = null,
        caption = null,
        bar = ProgressBar.None
    )
}

/** Link-only and Play-redirect apps ship no download candidates, so the
 * details screen falls back to this instead of a resolved candidate. Installed
 * Play-only apps still get the Open and Uninstall actions. */
internal fun linkButtonState(
    context: Context,
    availability: Availability,
    installed: Boolean = false,
    canOpen: Boolean = false
): InstallButtonState? = when (availability) {
    Availability.LINK_ONLY -> InstallButtonState(
        primary = ActionButton(
            context.getString(R.string.action_open_link),
            InstallAction.OPEN_LINK
        ),
        secondary = null,
        caption = null,
        bar = ProgressBar.None
    )

    Availability.PLAY_REDIRECT -> if (installed) {
        InstallButtonState(
            primary = ActionButton(
                context.getString(R.string.action_open),
                InstallAction.OPEN,
                enabled = canOpen
            ),
            secondary = ActionButton(
                context.getString(R.string.action_uninstall),
                InstallAction.UNINSTALL
            ),
            caption = null,
            bar = ProgressBar.None
        )
    } else {
        InstallButtonState(
            primary = ActionButton(
                context.getString(R.string.action_open_play),
                InstallAction.OPEN_STORE
            ),
            secondary = null,
            caption = null,
            bar = ProgressBar.None
        )
    }

    else -> null
}

private fun openButtonState(context: Context, canOpen: Boolean) = InstallButtonState(
    primary = ActionButton(
        context.getString(R.string.action_open),
        InstallAction.OPEN,
        enabled = canOpen
    ),
    secondary = ActionButton(
        context.getString(R.string.action_uninstall),
        InstallAction.UNINSTALL
    ),
    caption = null,
    bar = ProgressBar.None
)

internal fun installRefusalText(context: Context, refusal: InstallDispatch.Refused): String =
    refusal.status?.let {
        context.getString(R.string.install_refused, statusCaption(context, it))
    } ?: context.getString(R.string.install_refused_no_download)

internal fun requiresUnknownSourcesSettings(
    installer: Installer,
    canRequestPackageInstalls: Boolean
): Boolean = when (installer) {
    Installer.SESSION, Installer.NATIVE -> !canRequestPackageInstalls
    Installer.ROOT, Installer.SHIZUKU -> false
}

@Composable
internal fun InstallActions(
    state: InstallButtonState,
    onAction: (InstallAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = state,
        contentKey = { it.primary.action to it.secondary?.action },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "InstallActions"
    ) { current ->
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.spacing_large),
                    top = dimensionResource(R.dimen.spacing_medium),
                    end = dimensionResource(R.dimen.spacing_large),
                    bottom = dimensionResource(R.dimen.spacing_small)
                ),
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.spacing_medium)
            )
        ) {
            current.secondary?.let { secondary ->
                FilledTonalButton(
                    modifier = Modifier.weight(1F),
                    onClick = { onAction(secondary.action) },
                    enabled = secondary.enabled
                ) {
                    Text(
                        text = secondary.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Button(
                modifier = Modifier.weight(1F),
                onClick = { onAction(current.primary.action) },
                enabled = current.primary.enabled
            ) {
                Text(
                    text = current.primary.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
