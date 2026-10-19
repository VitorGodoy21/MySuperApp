package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseListUiStateTest {

    private val exercises = listOf(
        Exercise("1", "Supino reto", MuscleGroup.CHEST),
        Exercise("2", "Alongamento", null),
        Exercise("3", "Agachamento", MuscleGroup.LEGS),
        Exercise("4", "Crucifixo", MuscleGroup.CHEST),
    )

    @Test
    fun `groups follow MuscleGroup order with null last and names sorted`() {
        val groups = ExerciseListUiState(exercises = exercises).groups

        assertEquals(listOf(MuscleGroup.CHEST, MuscleGroup.LEGS, null), groups.map { it.muscleGroup })
        assertEquals(listOf("Crucifixo", "Supino reto"), groups[0].exercises.map { it.name })
        assertEquals("Sem grupo", groups.last().label)
    }

    @Test
    fun `query filters by name ignoring case and trims, dropping empty groups`() {
        val groups = ExerciseListUiState(exercises = exercises, query = "  AGACH ").groups

        assertEquals(1, groups.size)
        assertEquals(MuscleGroup.LEGS, groups.single().muscleGroup)
        assertEquals(listOf("Agachamento"), groups.single().exercises.map { it.name })
    }
}
