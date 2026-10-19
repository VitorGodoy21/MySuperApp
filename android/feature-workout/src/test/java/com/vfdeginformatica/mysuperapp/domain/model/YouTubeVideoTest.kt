package com.vfdeginformatica.mysuperapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeVideoTest {

    private val id = "dQw4w9WgXcQ"

    @Test
    fun `parses the usual youtube url shapes`() {
        listOf(
            "https://www.youtube.com/watch?v=$id",
            "https://youtube.com/watch?v=$id&t=42s",
            "https://m.youtube.com/watch?feature=share&v=$id",
            "https://youtu.be/$id",
            "https://youtu.be/$id?si=XyZ123",
            "youtu.be/$id",
            "www.youtube.com/watch?v=$id",
            "https://www.youtube.com/shorts/$id",
            "https://www.youtube.com/embed/$id?rel=0",
            "https://www.youtube.com/live/$id?feature=share",
            "https://www.youtube-nocookie.com/embed/$id",
            "  https://youtu.be/$id  ",
        ).forEach { url ->
            assertEquals("url: $url", id, YouTubeVideo.parseVideoId(url))
        }
    }

    @Test
    fun `rejects non youtube links, bad ids and blank input`() {
        listOf(
            "",
            "   ",
            "supino reto",
            "https://vimeo.com/123456",
            "https://example.com/watch?v=$id",
            "https://www.youtube.com/watch?list=PL123",
            "https://www.youtube.com/watch?v=short",
            "https://youtu.be/${id}X",
            "https://www.youtube.com/channel/UC123456789",
            "https://www.youtube.com/@someone",
        ).forEach { url ->
            assertNull("url: $url", YouTubeVideo.parseVideoId(url))
        }
    }

    @Test
    fun `canonical and embed urls are built from the id`() {
        assertEquals("https://www.youtube.com/watch?v=$id", YouTubeVideo.canonicalUrl(id))
        assertEquals(
            "https://www.youtube.com/embed/$id?playsinline=1&rel=0&modestbranding=1" +
                "&enablejsapi=1&origin=${YouTubeVideo.EMBED_ORIGIN}",
            YouTubeVideo.embedUrl(id)
        )
        assertEquals(id, Exercise(name = "x", videoUrl = YouTubeVideo.canonicalUrl(id)).videoId)
        assertNull(Exercise(name = "x").videoId)
    }

    @Test
    fun `only embed-related urls stay inside the in-app player`() {
        assertTrue(YouTubeVideo.isEmbedUrl(YouTubeVideo.embedUrl(id)))
        assertTrue(YouTubeVideo.isEmbedUrl("https://www.youtube-nocookie.com/embed/$id"))
        assertTrue(YouTubeVideo.isEmbedUrl("${YouTubeVideo.EMBED_ORIGIN}/"))
        assertTrue(YouTubeVideo.isEmbedUrl("https://i.ytimg.com/vi/$id/hqdefault.jpg"))
        assertFalse(YouTubeVideo.isEmbedUrl(YouTubeVideo.canonicalUrl(id)))
        assertFalse(YouTubeVideo.isEmbedUrl("https://www.youtube.com/shorts/$id"))
        assertFalse(YouTubeVideo.isEmbedUrl("https://youtu.be/$id"))
        assertFalse(YouTubeVideo.isEmbedUrl("not a url"))
    }
}
