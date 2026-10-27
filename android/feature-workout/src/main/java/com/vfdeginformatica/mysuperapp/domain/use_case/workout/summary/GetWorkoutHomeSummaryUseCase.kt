package com.vfdeginformatica.mysuperapp.domain.use_case.workout.summary

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.WeekDay
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.domain.repository.WorkoutRepository
import com.vfdeginformatica.mysuperapp.domain.repository.WorkoutSessionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.readFlow
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/** Gym summary shown on the Home card. */
data class WorkoutHomeSummary(
    /** Workout scheduled for today (first by name when several), ignoring the "Outros" group. */
    val todayWorkout: Workout?,
    val sessionsLast7Days: Int,
    val minutesLast7Days: Int,
    val caloriesLast7Days: Int,
)

class GetWorkoutHomeSummaryUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val sessionRepository: WorkoutSessionRepository,
) {
    companion object {
        const val WINDOW_DAYS = 7L
    }

    operator fun invoke(today: LocalDate = LocalDate.now()): Flow<Resource<WorkoutHomeSummary>> =
        readFlow("Erro ao carregar resumo da academia") {
            val workouts = workoutRepository.getWorkouts() ?: return@readFlow null
            val from = today.minusDays(WINDOW_DAYS - 1)
            val sessions = sessionRepository.getSessionsSince(from) ?: return@readFlow null
            build(workouts, sessions, today)
        }

    internal fun build(workouts: List<Workout>, sessions: List<WorkoutSession>, today: LocalDate): WorkoutHomeSummary {
        val weekDay = WeekDay.fromName(today.dayOfWeek.name)
        val from = today.minusDays(WINDOW_DAYS - 1)
        val recent = sessions.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
        return WorkoutHomeSummary(
            todayWorkout = workouts
                .filter { !it.isOther && it.dayOfWeek == weekDay }
                .minByOrNull { it.name.lowercase() },
            sessionsLast7Days = recent.size,
            minutesLast7Days = recent.sumOf { it.durationMinutes ?: 0 },
            caloriesLast7Days = recent.sumOf { it.calories ?: 0 },
        )
    }
}
