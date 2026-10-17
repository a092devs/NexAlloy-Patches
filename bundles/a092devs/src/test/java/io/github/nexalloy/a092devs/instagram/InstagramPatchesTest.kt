package io.github.nexalloy.a092devs.instagram

import io.github.nexalloy.a092devs.instagram.InstagramRequestFilter.Rule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.URI

class InstagramPatchesTest {

    @Test
    fun testRequestRules() {
        fun rule(url: String) = InstagramRequestFilter.ruleFor(URI(url))

        assertEquals(Rule.ANALYTICS, rule("https://graph.instagram.com/logging_client_events"))
        assertEquals(Rule.STORY_SEEN, rule("https://i.instagram.com/api/v2/media/seen/?reel=1"))
        assertEquals(Rule.LIVE_VIEWER, rule("https://i.instagram.com/api/v1/live/123/heartbeat_and_get_viewer_count/"))
        assertEquals(Rule.REELS_ADS, rule("https://i.instagram.com/api/v1/feed/injected_reels_media/"))
        assertEquals(Rule.ADS, rule("https://i.instagram.com/api/v1/async_ads/"))
        assertEquals(Rule.SUGGESTED, rule("https://i.instagram.com/api/v1/discover/chaining/"))
        assertNull(rule("https://i.instagram.com/api/v1/feed/timeline/"))

        val adsUri = URI("https://i.instagram.com/api/v1/async_ads/")
        val reelsAdsUri = URI("https://i.instagram.com/api/v1/feed/injected_reels_media/")
        InstagramRequestFilter.enable(Rule.ADS)
        InstagramRequestFilter.enable(Rule.REELS_ADS)
        assertTrue(InstagramRequestFilter.shouldBlock(adsUri))
        assertTrue(InstagramRequestFilter.shouldBlock(reelsAdsUri))
        assertFalse(InstagramRequestFilter.shouldBlock(URI("https://i.instagram.com/api/v1/feed/timeline/")))
    }

    @Test
    fun testSanitizeUrl() {
        assertEquals(
            "https://www.instagram.com/reel/abc/",
            InstagramLinks.sanitizeUrl("https://www.instagram.com/reel/abc/?igsh=MTc4MmM1YmI2Ng=="),
        )
        assertEquals(
            "https://www.instagram.com/p/abc/?img_index=2",
            InstagramLinks.sanitizeUrl("https://www.instagram.com/p/abc/?igsh=x&img_index=2"),
        )
        assertEquals(
            "https://example.com/?id=1#top",
            InstagramLinks.sanitizeUrl("https://example.com/?utm_source=ig&id=1&fbclid=x#top"),
        )
        assertEquals("https://example.com/a", InstagramLinks.sanitizeUrl("https://example.com/a"))
        assertEquals("not a url", InstagramLinks.sanitizeUrl("not a url"))
    }
}

