package io.github.nexalloy.a092devs.instagram

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URI
import java.util.Locale

/**
 * Link sanitizer and external browser dispatcher ported from SysAdminDoc/HushGram (GPL-3.0).
 */
object InstagramLinks {
    private const val FIRST_PARTY_DOMAIN = "instagram.com"

    /** Query parameters of Instagram links that change what is opened. */
    private val functionalParameters = setOf("comment_id", "img_index", "open_comments", "story_media_id")

    /** Query parameters of links that only track the user or share context. */
    private val trackingParameters = setOf(
        "igsh", "igsi", "utm_source", "utm_medium", "utm_campaign",
        "utm_content", "utm_term", "fbclid", "si", "ref", "ref_src"
    )

    /**
     * Removes tracking parameters from external URLs and preserves only functional
     * parameters for first-party Instagram URLs.
     */
    fun sanitizeUrl(url: String): String = try {
        val uri = URI(url)
        val host = uri.host
        val rawQuery = uri.rawQuery
        if (host == null || rawQuery == null) {
            url
        } else {
            val normalizedHost = host.lowercase(Locale.ROOT)
            val firstParty = normalizedHost == FIRST_PARTY_DOMAIN || normalizedHost.endsWith(".$FIRST_PARTY_DOMAIN")
            val parameters = rawQuery.split('&').filter { parameter ->
                val name = parameter.substringBefore('=')
                when {
                    parameter.isEmpty() -> !firstParty
                    firstParty -> name in functionalParameters
                    else -> name !in trackingParameters
                }
            }
            val query = parameters.joinToString("&")
            if (query == rawQuery) url else replaceQuery(url, query, parameters.isNotEmpty())
        }
    } catch (_: Exception) {
        url
    }

    private fun replaceQuery(url: String, query: String, keepQueryDelimiter: Boolean): String {
        val queryStart = url.indexOf('?')
        if (queryStart < 0) return url
        val fragmentStart = url.indexOf('#', queryStart)
        val fragment = if (fragmentStart < 0) "" else url.substring(fragmentStart)
        val base = url.substring(0, queryStart)
        return if (keepQueryDelimiter) "$base?$query$fragment" else "$base$fragment"
    }

    /**
     * Opens an Instagram redirect link or web URL in an external browser.
     */
    fun openExternally(context: Context, url: String): Boolean {
        val target = runCatching { Uri.parse(url).getQueryParameter("u") }.getOrNull() ?: url
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(sanitizeUrl(target)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }.isSuccess
    }
}

