package com.vfdeginformatica.mysuperapp.domain.model

data class Exercise(
    val id: String = "",
    val name: String,
    val muscleGroup: MuscleGroup? = null,
    val description: String = "",
    val notes: String = "",
    /** Canonical YouTube URL (`https://www.youtube.com/watch?v=<id>`) or empty when there is no video. */
    val videoUrl: String = "",
) {
    val videoId: String?
        get() = YouTubeVideo.parseVideoId(videoUrl)
}
