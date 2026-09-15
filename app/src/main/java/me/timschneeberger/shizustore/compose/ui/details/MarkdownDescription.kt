/*
 * SPDX-FileCopyrightText: 2026 Tim Schneeberger
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package me.timschneeberger.shizustore.compose.ui.details

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.elements.MarkdownCheckBox
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import java.net.URI
import me.timschneeberger.shizustore.extensions.viewExternal

/**
 * Renders the upstream README. Rows that carry GitHub rendered HTML instead of
 * markdown fall back to the platform HTML renderer.
 */
@Composable
internal fun MarkdownDescription(
    content: String,
    repoBaseUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = remember(context) {
        object : UriHandler {
            override fun openUri(uri: String) {
                context.viewExternal(uri)
            }
        }
    }

    // The library annotates links against LocalUriHandler; override it so URLs
    // fail soft like every other external link in the app.
    CompositionLocalProvider(LocalUriHandler provides uriHandler) {
        if (isRenderedHtml(content)) {
            Text(
                text = remember(content) { AnnotatedString.fromHtml(content) },
                modifier = modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            val rendered = remember(content, repoBaseUrl) {
                normalizeReadmeImages(content, repoBaseUrl)
            }
            // Shift the default heading scale down one step; the display sizes
            // dominate a phone-sized screen.
            val typography = markdownTypography(
                h1 = MaterialTheme.typography.displayMedium,
                h2 = MaterialTheme.typography.displaySmall,
                h3 = MaterialTheme.typography.headlineMedium,
                h4 = MaterialTheme.typography.headlineSmall,
                h5 = MaterialTheme.typography.titleLarge,
                h6 = MaterialTheme.typography.titleMedium
            )
            val components = markdownComponents(
                checkbox = { MarkdownCheckBox(it.content, it.node, it.typography.text) },
                codeBlock = {
                    MarkdownHighlightedCodeBlock(it.content, it.node, showHeader = true)
                },
                codeFence = {
                    MarkdownHighlightedCodeFence(it.content, it.node, showHeader = true)
                },
                table = { model ->
                    // Cells ellipsize by default; let them wrap instead so long
                    // text stays readable while the table remains scrollable.
                    MarkdownTable(
                        content = model.content,
                        node = model.node,
                        style = model.typography.table,
                        headerBlock = { cellContent, header, tableWidth, cellStyle ->
                            MarkdownTableHeader(
                                content = cellContent,
                                header = header,
                                tableWidth = tableWidth,
                                style = cellStyle,
                                maxLines = Int.MAX_VALUE,
                                overflow = TextOverflow.Clip
                            )
                        },
                        rowBlock = { rowContent, row, tableWidth, cellStyle ->
                            MarkdownTableRow(
                                content = rowContent,
                                header = row,
                                tableWidth = tableWidth,
                                style = cellStyle,
                                maxLines = Int.MAX_VALUE,
                                overflow = TextOverflow.Clip
                            )
                        }
                    )
                }
            )
            Markdown(
                content = rendered,
                modifier = modifier.fillMaxWidth(),
                typography = typography,
                imageTransformer = NoCacheImageTransformer,
                components = components
            )
        }
    }
}

private val renderedHtmlMarkers = listOf(
    "id=\"readme\"",
    "data-path=",
    "class=\"markdown-body\"",
    "class=\"markdown-heading\""
)

internal fun isRenderedHtml(content: String): Boolean = renderedHtmlMarkers.any(content::contains)

private val htmlImageRegex = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE)

private val htmlAttributeRegex = Regex(
    "(?i)\\b(src|alt)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))"
)

// Matches the URL token of a markdown image, keeping a wrapping <...> optional.
private val markdownImageRegex = Regex("!\\[[^\\]]*\\]\\(\\s*(<[^>]*>|[^)\\s]+)")

private val urlSchemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

private val githubRepoUrlRegex = Regex(
    "https?://github\\.com/([^/]+)/([^/?#]+)",
    RegexOption.IGNORE_CASE
)

/**
 * GitHub READMEs put images in two shapes the markdown renderer cannot resolve
 * on its own: raw HTML <img> tags (dropped with the HTML block) and relative
 * URLs (useless without a ref). Rewrite both so every image reaches Coil.
 */
internal fun normalizeReadmeImages(markdown: String, repoBaseUrl: String?): String {
    if (markdown.isBlank()) return markdown

    var normalized = replaceHtmlImages(markdown)
    if (repoBaseUrl != null) {
        normalized = resolveMarkdownImages(normalized, repoBaseUrl)
    }
    return normalized
}

internal fun githubRawBase(sourceUrl: String?): String? {
    val url = sourceUrl ?: return null
    val match = githubRepoUrlRegex.find(url) ?: return null
    val owner = match.groupValues[1]
    val repo = match.groupValues[2].removeSuffix(".git")
    return "https://raw.githubusercontent.com/$owner/$repo/HEAD/"
}

private fun replaceHtmlImages(markdown: String): String =
    htmlImageRegex.replace(markdown) { match ->
        val tag = match.value
        val src = htmlAttribute(tag, "src")
        if (src.isNullOrBlank()) {
            ""
        } else {
            val alt = htmlAttribute(tag, "alt").orEmpty()
                .replace("]", "\\]")
                .replace("\n", " ")
            // Blank lines around the image keep it out of the surrounding HTML
            // block, which the renderer otherwise drops entirely.
            "\n\n![$alt]($src)\n\n"
        }
    }

private fun htmlAttribute(tag: String, name: String): String? {
    for (match in htmlAttributeRegex.findAll(tag)) {
        if (!match.groupValues[1].equals(name, ignoreCase = true)) continue
        val doubleQuoted = match.groupValues[2]
        val singleQuoted = match.groupValues[3]
        return doubleQuoted.ifEmpty { singleQuoted }.ifEmpty { match.groupValues[4] }
    }
    return null
}

private fun resolveMarkdownImages(markdown: String, repoBaseUrl: String): String =
    markdownImageRegex.replace(markdown) { match ->
        val token = match.groupValues[1]
        // Match ranges are absolute in the input; rebase onto the match body.
        val start = match.groups[1]!!.range.first - match.range.first
        val replacement = resolveImageToken(repoBaseUrl, token)
        match.value.replaceRange(start, start + token.length, replacement)
    }

private fun resolveImageToken(repoBaseUrl: String, token: String): String {
    if (token.startsWith("<") && token.endsWith(">")) {
        val inner = token.substring(1, token.length - 1)
        return if (isRelative(inner)) "<${resolveRelative(repoBaseUrl, inner)}>" else token
    }
    return if (isRelative(token)) resolveRelative(repoBaseUrl, token) else token
}

private fun isRelative(url: String): Boolean = url.isNotBlank() &&
    !url.startsWith("//") &&
    !url.startsWith("#") &&
    !urlSchemeRegex.containsMatchIn(url)

private fun resolveRelative(repoBaseUrl: String, url: String): String = try {
    URI(repoBaseUrl).resolve(url.removePrefix("/")).toString()
} catch (_: Exception) {
    url
}

/**
 * README images are one-off decoration; caching them would grow the shared disk
 * cache with hundreds of badges, so both caches are disabled for these loads.
 */
private object NoCacheImageTransformer : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData {
        val painter = rememberAsyncImagePainter(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(link)
                .size(coil3.size.Size.ORIGINAL)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .build()
        )
        return ImageData(painter)
    }
}
