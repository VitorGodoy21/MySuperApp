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

    /**
     * Origin reported to YouTube as `Referer`/`origin` when embedding. Since 2025 the IFrame
     * player refuses to play (errors 152-x/153) when the embedding page has no valid HTTP
     * referer; `youtube.com` itself is not accepted, so the app's own Firebase Hosting site is used.
     */
    const val EMBED_ORIGIN = "https://baila-675c6.web.app"

    fun canonicalUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    fun embedUrl(videoId: String): String =
        "https://www.youtube.com/embed/$videoId" +
            "?playsinline=1&rel=0&modestbranding=1&enablejsapi=1&origin=$EMBED_ORIGIN"

    /** True for URLs the in-app player may load itself (the embed page and its helpers). */
    fun isEmbedUrl(url: String): Boolean {
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return url.startsWith(EMBED_ORIGIN) ||
            (host in HOSTS && uri.rawPath.orEmpty().startsWith("/embed/")) ||
            host.endsWith("google.com") || host.endsWith("googlevideo.com") ||
            host.endsWith("ytimg.com") || host.endsWith("gstatic.com")
    }

    private fun queryParam(rawQuery: String?, key: String): String? =
        rawQuery?.split('&')
            ?.map { it.substringBefore('=') to it.substringAfter('=', "") }
            ?.firstOrNull { it.first == key }
            ?.second
}
