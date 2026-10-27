package com.vfdeginformatica.mysuperapp.domain.use_case.workout.summary

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.WeekDay
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.domain.repository.FakeWorkoutRepository
import com.vfdeginformatica.mysuperapp.domain.repository.FakeWorkoutSessionRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GetWorkoutHomeSummaryUseCaseTest {

    // A Wednesday
    private val today: LocalDate = LocalDate.of(2026, 10, 28)

    private fun session(id: String, daysAgo: Long, minutes: Int? = null, kcal: Int? = null) =
        WorkoutSession(id = id, workoutId = "w", date = today.minusDays(daysAgo), durationMinutes = minutes, calories = kcal)

    @Test
    fun `picks today's workout ignoring Outros and prefers name order`() = runTest {
        val workouts = listOf(
            Workout(id = "b", name = "Treino B", dayOfWeek = WeekDay.WEDNESDAY),
            Workout(id = "a", name = "Treino A", dayOfWeek = WeekDay.WEDNESDAY),
            Workout(id = "o", name = "Aaa outros", dayOfWeek = WeekDay.WEDNESDAY, isOther = true),
            Workout(id = "c", name = "Treino C", dayOfWeek = WeekDay.THURSDAY),
        )
        val useCase = GetWorkoutHomeSummaryUseCase(FakeWorkoutRepository(workouts), FakeWorkoutSessionRepository())

        val result = useCase(today).toList().last()

        assertTrue(result is Resource.Success)
        assertEquals("a", result.data!!.todayWorkout?.id)
    }

    @Test
    fun `no workout scheduled today yields null`() = runTest {
        val useCase = GetWorkoutHomeSummaryUseCase(
            FakeWorkoutRepository(listOf(Workout(id = "c", name = "C", dayOfWeek = WeekDay.THURSDAY))),
            FakeWorkoutSessionRepository(),
        )

        assertNull(useCase(today).toList().last().data!!.todayWorkout)
    }

    @Test
    fun `sums last 7 days ignoring nulls and older sessions`() = runTest {
        val sessions = listOf(
            session("1", 0, minutes = 60, kcal = 400),
            session("2", 3, minutes = null, kcal = 200),
            session("3", 6, minutes = 30, kcal = null),
            session("4", 7, minutes = 999, kcal = 999),
        )
        val useCase = GetWorkoutHomeSummaryUseCase(FakeWorkoutRepository(), FakeWorkoutSessionRepository(sessions))

        val summary = useCase(today).toList().last().data!!

        assertEquals(3, summary.sessionsLast7Days)
        assertEquals(90, summary.minutesLast7Days)
        assertEquals(600, summary.caloriesLast7Days)
    }

    @Test
    fun `read failure is an error`() = runTest {
        val useCase = GetWorkoutHomeSummaryUseCase(
            FakeWorkoutRepository().apply { failReads = true },
            FakeWorkoutSessionRepository(),
        )

        assertTrue(useCase(today).toList().last() is Resource.Error)
    }
}
