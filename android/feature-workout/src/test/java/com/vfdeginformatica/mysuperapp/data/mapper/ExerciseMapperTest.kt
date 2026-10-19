package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.ExerciseDto
import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseMapperTest {

    @Test
    fun `unknown muscle group falls back to OTHER`() {
        val model = ExerciseDto(id = "e1", name = "Supino", muscleGroup = "WINGS").toModel()

        assertEquals(MuscleGroup.OTHER, model.muscleGroup)
    }

    @Test
    fun `blank muscle group maps to null and back to empty string`() {
        val model = ExerciseDto(id = "e1", name = "Supino", muscleGroup = "").toModel()

        assertNull(model.muscleGroup)
        assertEquals("", model.toDto().muscleGroup)
    }

    @Test
    fun `video url round-trips and defaults to empty for legacy documents`() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val model = ExerciseDto(id = "e1", name = "Supino", videoUrl = url).toModel()

        assertEquals(url, model.videoUrl)
        assertEquals("dQw4w9WgXcQ", model.videoId)
        assertEquals(url, model.toDto().videoUrl)

        val legacy = ExerciseDto(id = "e2", name = "Remada").toModel()
        assertEquals("", legacy.videoUrl)
        assertNull(legacy.videoId)
    }
}
