package com.vfdeginformatica.mysuperapp.presentation.screen.home.contract

import androidx.fragment.app.FragmentActivity

sealed interface HomeEvent {
    data class OnOpenFinance(val activity: FragmentActivity?) : HomeEvent
    data class OnRevealFinance(val activity: FragmentActivity?) : HomeEvent
    data object OnHideFinance : HomeEvent
    data class OnAddExpense(val activity: FragmentActivity?) : HomeEvent
    data class OnAddIncome(val activity: FragmentActivity?) : HomeEvent

    data class OnOpenQrCodes(val activity: FragmentActivity?) : HomeEvent
    data class OnOpenNfc(val activity: FragmentActivity?) : HomeEvent

    data object OnOpenWorkoutHub : HomeEvent
    data class OnOpenTodayWorkout(val workoutId: String) : HomeEvent
    data object OnOpenDiet : HomeEvent

    data object OnRefresh : HomeEvent
}
