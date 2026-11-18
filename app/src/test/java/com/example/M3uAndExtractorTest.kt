package com.example

import com.example.data.remote.M3uParser
import com.example.data.remote.MediaDiscoveryService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uAndExtractorTest {

    @Test
    fun `M3uParser extracts channels with logos and groups`() {
        val sample = """
            #EXTM3U
            #EXTINF:-1 tvg-logo="https://example.com/logo1.png" group-title="Cinema",Classic Movies HD
            #EXTVLCOPT:network-caching=1000
            https://example.com/streams/classic.m3u8
            #EXTINF:-1 group-title="Docs",Science Channel
            https://example.com/streams/science.mp4
        """.trimIndent()

        val channels = M3uParser.parse(sample)
        assertEquals(2, channels.size)
        assertEquals("Classic Movies HD", channels[0].title)
        assertEquals("https://example.com/logo1.png", channels[0].logoUrl)
        assertEquals("Cinema", channels[0].groupTitle)
        assertEquals("https://example.com/streams/classic.m3u8", channels[0].streamUrl)
        assertEquals("Science Channel", channels[1].title)
        assertEquals("Docs", channels[1].groupTitle)
    }

    @Test
    fun `MediaDiscoveryService extracts video streams from HTML`() {
        val html = """
            <html>
              <head>
                <title>Test Movie Page</title>
                <meta property="og:video" content="https://cdn.example.com/movie/master.m3u8" />
              </head>
              <body>
                <video src="https://cdn.example.com/movie/720p.mp4"></video>
              </body>
            </html>
        """.trimIndent()

        val service = MediaDiscoveryService()
        val streams = service.scanHtmlForStreams(html, "https://example.com/watch")
        assertEquals(2, streams.size)
        assertEquals("HLS", streams[0].formatBadge)
        assertTrue(streams.any { it.url.endsWith("720p.mp4") && it.formatBadge == "MP4" })
    }

    @Test
    fun `MediaDiscoveryService parses DuckDuckGo search results`() {
        val html = """
            <div class="result">
              <a class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Farchive.org%2Fdetails%2Fsintel">Sintel Full Movie</a>
            </div>
        """.trimIndent()

        val service = MediaDiscoveryService()
        val hits = service.parseDuckDuckGoHtml(html)
        assertEquals(1, hits.size)
        assertEquals("https://archive.org/details/sintel", hits[0].url)
        assertEquals("archive.org", hits[0].domain)
    }
}
