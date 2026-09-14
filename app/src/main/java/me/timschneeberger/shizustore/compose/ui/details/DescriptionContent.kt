/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import me.timschneeberger.shizustore.R
import me.timschneeberger.shizustore.extensions.viewExternal

/**
 * A chunk of the upstream README. Code blocks are split out because the platform HTML
 * converter has no support for [pre]/[code]; images become their alt text because the
 * converter otherwise emits an object replacement character.
 */
internal sealed interface DescriptionSegment {
    data class Text(val html: String) : DescriptionSegment
    data class Code(val language: String?, val code: String) : DescriptionSegment
}

private val htmlTagRegex = Regex("<[a-zA-Z/][^>]*>")

internal fun isPlainText(text: String): Boolean = !htmlTagRegex.containsMatchIn(text)

private val dotAllAndIgnoreCase = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
private val preBlockRegex = Regex("<pre\\b[^>]*>(.*?)</pre>", dotAllAndIgnoreCase)
private val svgRegex = Regex("<svg\\b.*?</svg>", dotAllAndIgnoreCase)
private val headingAnchorRegex =
    Regex("<a\\b[^>]*class=\"[^\"]*anchor[^\"]*\"[^>]*>.*?</a>", dotAllAndIgnoreCase)
private val inputRegex = Regex("<input\\b[^>]*>", dotAllAndIgnoreCase)
private val imageRegex = Regex("<img\\b[^>]*>", dotAllAndIgnoreCase)
private val imageAltRegex = Regex("\\balt=\"([^\"]*)\"", RegexOption.IGNORE_CASE)
private val codeOpenRegex = Regex("<code\\b[^>]*>", RegexOption.IGNORE_CASE)
private val codeCloseRegex = Regex("</code\\s*>", RegexOption.IGNORE_CASE)
private val anyTagRegex = Regex("<[^>]*>")
private val trailingOpenDivRegex = Regex("<div\\b[^>]*>\\s*$", RegexOption.IGNORE_CASE)
private val leadingCloseDivRegex = Regex("^\\s*</div\\s*>", RegexOption.IGNORE_CASE)
private val highlightLanguageRegex =
    Regex("highlight-(?:source|text)-([A-Za-z0-9+#._-]+)", RegexOption.IGNORE_CASE)
private val entityRegex = Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z][a-zA-Z0-9]*);")

private val namedEntities = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to "\u00A0",
    "hellip" to "\u2026"
)

internal fun parseDescription(html: String): List<DescriptionSegment> {
    if (isPlainText(html)) return listOf(DescriptionSegment.Text(html))

    val segments = mutableListOf<DescriptionSegment>()
    var cursor = 0
    for (match in preBlockRegex.findAll(html)) {
        val (language, plainBefore) = splitTrailingHighlightDiv(
            html.substring(cursor, match.range.first)
        )
        val prepared = prepareText(plainBefore)
        if (isVisible(prepared)) segments += DescriptionSegment.Text(prepared)
        val code = decodeHtmlEntities(anyTagRegex.replace(match.groupValues[1], "")).trim('\n')
        segments += DescriptionSegment.Code(language, code)
        cursor = match.range.last + 1
    }
    val tail = prepareText(html.substring(cursor))
    if (isVisible(tail)) segments += DescriptionSegment.Text(tail)
    return segments
}

/**
 * GitHub wraps a code block in a `highlight-source-<lang>` or `highlight-text-<lang>` div; other
 * blocks use a plain wrapper. Either way the opening tag directly precedes `<pre>`.
 */
private fun splitTrailingHighlightDiv(before: String): Pair<String?, String> {
    val trimmed = before.trimEnd()
    val openTagStart = trailingOpenDivRegex.find(trimmed)?.range?.first ?: return null to before
    val language = highlightLanguageRegex.find(trimmed.substring(openTagStart))
        ?.groupValues?.get(1)?.lowercase()
    return language to trimmed.substring(0, openTagStart)
}

private fun prepareText(fragment: String): String {
    if (fragment.isBlank()) return ""
    var text = leadingCloseDivRegex.replace(fragment, "")
    text = svgRegex.replace(text, "")
    text = headingAnchorRegex.replace(text, "")
    text = inputRegex.replace(text) { match ->
        if (match.value.contains("checked", ignoreCase = true)) "\u2611" else "\u2610"
    }
    text = imageRegex.replace(text) { match ->
        imageAltRegex.find(match.value)?.groupValues?.get(1).orEmpty()
    }
    text = codeOpenRegex.replace(text, "<tt>")
    return codeCloseRegex.replace(text, "</tt>")
}

private fun isVisible(html: String): Boolean =
    decodeHtmlEntities(anyTagRegex.replace(html, "")).isNotBlank()

internal fun decodeHtmlEntities(text: String): String {
    if (!text.contains('&')) return text
    return entityRegex.replace(text) { match ->
        val entity = match.groupValues[1]
        namedEntities[entity.lowercase()] ?: decodeNumericEntity(entity) ?: match.value
    }
}

private fun decodeNumericEntity(entity: String): String? {
    val isHex = entity.startsWith("#x", ignoreCase = true)
    val digits = if (isHex) entity.drop(2) else entity.drop(1)
    val codePoint = digits.toIntOrNull(if (isHex) 16 else 10) ?: return null
    if (codePoint !in 0..0x10FFFF) return null
    return String(Character.toChars(codePoint))
}

@Composable
internal fun rememberDescription(
    raw: String,
    linkStyles: TextLinkStyles? = null,
    linkInteractionListener: LinkInteractionListener? = null
): AnnotatedString = remember(raw, linkStyles, linkInteractionListener) {
    if (isPlainText(raw)) {
        AnnotatedString(raw)
    } else {
        AnnotatedString.fromHtml(raw, linkStyles, linkInteractionListener)
    }
}

@Composable
internal fun DescriptionBody(html: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val segments = remember(html) { parseDescription(html) }
    val linkColor = MaterialTheme.colorScheme.primary
    val linkStyles = remember(linkColor) {
        TextLinkStyles(
            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
        )
    }
    val linkInteractionListener = remember(context) {
        LinkInteractionListener { link ->
            (link as? LinkAnnotation.Url)?.url?.let(context::viewExternal)
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        segments.forEach { segment ->
            when (segment) {
                is DescriptionSegment.Text -> Text(
                    text = rememberDescription(segment.html, linkStyles, linkInteractionListener),
                    style = MaterialTheme.typography.bodyMedium
                )

                is DescriptionSegment.Code -> CodeBlock(segment)
            }
        }
    }
}

@Composable
private fun CodeBlock(segment: DescriptionSegment.Code) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(R.dimen.radius_small)))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(
                    horizontal = dimensionResource(R.dimen.spacing_medium),
                    vertical = dimensionResource(R.dimen.spacing_small)
                )
        ) {
            segment.language?.let { language ->
                Text(
                    text = language,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = segment.code,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
