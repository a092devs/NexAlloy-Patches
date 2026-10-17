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

    @Test
    fun testDashManifestAudioExtraction() {
        val sampleMpd = """
            <MPD xmlns="urn:mpeg:dash:schema:mpd:2011" minBufferTime="PT1.500S">
              <Period duration="PT0H0M28.028S">
                <AdaptationSet segmentAlignment="true" maxWidth="720" maxHeight="1280">
                  <Representation id="1" mimeType="video/mp4" codecs="avc1.64001F" bandwidth="1158670">
                    <BaseURL>https://video.xx.fbcdn.net/video.mp4</BaseURL>
                  </Representation>
                </AdaptationSet>
                <AdaptationSet segmentAlignment="true" mimeType="audio/mp4">
                  <Representation id="2" codecs="mp4a.40.2" bandwidth="131072">
                    <BaseURL>https://video.xx.fbcdn.net/audio_best.m4a?a=1&amp;b=2</BaseURL>
                  </Representation>
                  <Representation id="3" codecs="mp4a.40.5" bandwidth="64000">
                    <BaseURL>https://video.xx.fbcdn.net/audio_low.m4a</BaseURL>
                  </Representation>
                </AdaptationSet>
              </Period>
            </MPD>
        """.trimIndent()

        val audioUrl = io.github.nexalloy.a092devs.instagram.download.DashManifest.bestAudioUrl(sampleMpd)
        assertEquals("https://video.xx.fbcdn.net/audio_best.m4a?a=1&b=2", audioUrl)

        assertNull(io.github.nexalloy.a092devs.instagram.download.DashManifest.bestAudioUrl(null))
        assertNull(io.github.nexalloy.a092devs.instagram.download.DashManifest.bestAudioUrl(""))
        assertNull(io.github.nexalloy.a092devs.instagram.download.DashManifest.bestAudioUrl("<MPD></MPD>"))
    }
}

