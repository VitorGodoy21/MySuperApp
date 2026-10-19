package com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.WeekDay
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutListUiStateTest {

    private val exercises = listOf(
        Exercise(id = "e1", name = "Supino"),
        Exercise(id = "e2", name = "Agachamento"),
        Exercise(id = "e3", name = "Remada"),
        Exercise(id = "e4", name = "Rosca"),
    )

    @Test
    fun `preview shows first three names and a plus counter`() {
        val state = WorkoutListUiState(
            workouts = listOf(
                Workout(
                    id = "w1", name = "A", dayOfWeek = WeekDay.FRIDAY,
                    items = listOf("e1", "e2", "e3", "e4").mapIndexed { i, id -> WorkoutItem(id, i) },
                )
            ),
            exercises = exercises,
        )

        val row = state.rows.single()
        assertEquals("Supino • Agachamento • Remada +1", row.preview)
        assertEquals(4, row.exerciseCount)
        assertEquals("Sexta-feira", row.dayLabel)
    }

    @Test
    fun `preview labels unknown exercise ids as removed`() {
        val state = WorkoutListUiState(
            workouts = listOf(
                Workout(id = "w1", name = "A", items = listOf(WorkoutItem("e1", 0), WorkoutItem("gone", 1)))
            ),
            exercises = exercises,
        )

        val row = state.rows.single()
        assertEquals("Supino • Exercício removido", row.preview)
        assertNull(row.dayLabel)
    }
}
