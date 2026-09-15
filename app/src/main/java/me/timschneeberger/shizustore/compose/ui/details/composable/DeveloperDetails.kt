/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details.composable

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.compose.composable.Info
import me.timschneeberger.shizustore.compose.composable.SectionHeader
import me.timschneeberger.shizustore.data.model.AppDetails
import me.timschneeberger.shizustore.extensions.copyToClipBoard
import me.timschneeberger.shizustore.extensions.isTAndAbove
import me.timschneeberger.shizustore.extensions.viewExternal

@Composable
fun DeveloperDetails(
    details: AppDetails,
    modifier: Modifier = Modifier,
    onCopied: (String) -> Unit = {}
) {
    val context = LocalContext.current

    val entries = buildList {
        details.authorName.entry(R.string.details_developer_name, R.drawable.ic_person)
            ?.let(::add)

        details.authorUrl.entry(R.string.details_developer_profile, R.drawable.ic_person) {
            context.viewExternal(it)
        }?.let(::add)

        details.authorEmail.entry(R.string.details_developer_email, R.drawable.ic_mail) {
            context.viewExternal("mailto:$it")
        }?.let(::add)

        details.authorPhone.entry(R.string.details_developer_phone, R.drawable.ic_phone) {
            context.viewExternal("tel:$it")
        }?.let(::add)

        details.authorWebSite.entry(
            R.string.details_developer_website,
            R.drawable.ic_language
        ) { context.viewExternal(it) }?.let(::add)

        details.liberapay.entry(
            R.string.details_donate_liberapay,
            R.drawable.ic_volunteer_activism
        ) { context.viewExternal(LIBERAPAY_URL + it) }?.let(::add)

        details.openCollective.entry(
            R.string.details_donate_open_collective,
            R.drawable.ic_volunteer_activism
        ) { context.viewExternal(OPEN_COLLECTIVE_URL + it) }?.let(::add)

        details.bitcoin.entry(R.string.details_donate_bitcoin, R.drawable.ic_group_wallets) {
            context.copy(it, onCopied)
        }?.let(::add)

        details.litecoin.entry(R.string.details_donate_litecoin, R.drawable.ic_group_wallets) {
            context.copy(it, onCopied)
        }?.let(::add)
    }

    if (entries.isEmpty()) return

    Column(modifier = modifier) {
        SectionHeader(title = stringResource(R.string.details_developer))

        entries.forEach { entry ->
            Info(
                title = stringResource(entry.labelRes),
                description = entry.value,
                painter = painterResource(entry.iconRes),
                onClick = entry.onClick
            )
        }
    }
}

/** Android 13 shows its own clipboard preview, so a second confirmation is only ours to give. */
private fun Context.copy(value: String, onCopied: (String) -> Unit) {
    copyToClipBoard(value)
    if (!isTAndAbove) onCopied(value)
}

private data class DeveloperEntry(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val value: String,
    val onClick: (() -> Unit)?
)

private fun String?.entry(
    @StringRes labelRes: Int,
    @DrawableRes iconRes: Int,
    onClick: ((String) -> Unit)? = null
): DeveloperEntry? {
    val value = this?.takeIf { it.isNotBlank() } ?: return null
    return DeveloperEntry(labelRes, iconRes, value, onClick?.let { action -> { action(value) } })
}

private const val LIBERAPAY_URL = "https://liberapay.com/"

private const val OPEN_COLLECTIVE_URL = "https://opencollective.com/"
