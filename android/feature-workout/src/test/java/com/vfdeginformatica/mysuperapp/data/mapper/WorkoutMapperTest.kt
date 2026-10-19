package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutItemDto
import com.vfdeginformatica.mysuperapp.domain.model.WeekDay
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutMapperTest {

    @Test
    fun `toDto derives distinct exerciseIds and re-indexes order from position`() {
        val workout = Workout(
            id = "w1",
            name = "Treino A",
            items = listOf(
                WorkoutItem("e2", order = 7),
                WorkoutItem("e1", order = 3),
                WorkoutItem("e2", order = 9),
            ),
        )

        val dto = workout.toDto()

        assertEquals(listOf("e2", "e1"), dto.exerciseIds)
        assertEquals(listOf(0, 1, 2), dto.items.map { it.order })
        assertEquals(listOf("e2", "e1", "e2"), dto.items.map { it.exerciseId })
    }

    @Test
    fun `toModel sorts items by order and re-indexes`() {
        val dto = WorkoutDto(
            id = "w1",
            name = "Treino A",
            dayOfWeek = "MONDAY",
            items = listOf(
                WorkoutItemDto(exerciseId = "e3", order = 5),
                WorkoutItemDto(exerciseId = "e1", order = 1),
                WorkoutItemDto(exerciseId = "e2", order = 3),
            ),
        )

        val model = dto.toModel()

        assertEquals(WeekDay.MONDAY, model.dayOfWeek)
        assertEquals(listOf("e1", "e2", "e3"), model.items.map { it.exerciseId })
        assertEquals(listOf(0, 1, 2), model.items.map { it.order })
    }

    @Test
    fun `round trip preserves prescription fields`() {
        val original = Workout(
            id = "w1",
            name = "Treino A",
            dayOfWeek = WeekDay.SATURDAY,
            notes = "leve",
            items = listOf(
                WorkoutItem("e1", 0, sets = 3, reps = "10-12", loadKg = 20.5, restSeconds = 60, notes = "n"),
                WorkoutItem("e2", 1),
            ),
        )

        val restored = original.toDto().toModel()

        assertEquals(original, restored)
    }
}
