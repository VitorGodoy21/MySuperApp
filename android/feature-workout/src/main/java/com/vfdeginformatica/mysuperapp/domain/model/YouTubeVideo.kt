package com.vfdeginformatica.mysuperapp.domain.model

/** Helpers for the single YouTube video that can be attached to an [Exercise]. */
object YouTubeVideo {

    private val VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")

    private val HOSTS = setOf(
        "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com",
        "youtu.be", "www.youtu.be", "youtube-nocookie.com", "www.youtube-nocookie.com",
    )

    private val PATH_PREFIXES = listOf("/shorts/", "/embed/", "/live/", "/v/", "/e/")

    /**
     * Extracts the 11-char video id from the usual YouTube URL shapes
     * (`watch?v=`, `youtu.be/`, `shorts/`, `embed/`, `live/`, with or without scheme/`www.`,
     * ignoring extra query params such as `t=` or `si=`). Returns null when the text is not a
     * recognizable YouTube video link.
     */
    fun parseVideoId(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null

        val withScheme = if (trimmed.contains("://")) trimmed else "https://$trimmed"
        val uri = runCatching { java.net.URI(withScheme) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        if (host !in HOSTS) return null

        val path = uri.rawPath.orEmpty()
        val candidate: String? = when {
            host.endsWith("youtu.be") -> path.trimStart('/').substringBefore('/')
            path == "/watch" || path == "/watch/" -> queryParam(uri.rawQuery, "v")
            else -> PATH_PREFIXES.firstOrNull { path.startsWith(it) }
                ?.let { prefix -> path.removePrefix(prefix).substringBefore('/') }
        }
        return candidate?.takeIf { VIDEO_ID_REGEX.matches(it) }
    }

    fun canonicalUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    fun embedUrl(videoId: String): String =
        "https://www.youtube.com/embed/$videoId?playsinline=1&rel=0&modestbranding=1"

    private fun queryParam(rawQuery: String?, key: String): String? =
        rawQuery?.split('&')
            ?.map { it.substringBefore('=') to it.substringAfter('=', "") }
            ?.firstOrNull { it.first == key }
            ?.second
}
