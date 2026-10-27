package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.vfdeginformatica.mysuperapp.presentation.screen.home.components.FinanceTopicCard
import com.vfdeginformatica.mysuperapp.presentation.screen.home.components.SmartTagsTopicCard
import com.vfdeginformatica.mysuperapp.presentation.screen.home.components.WorkoutTopicCard
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    innerPadding: PaddingValues,
    activity: FragmentActivity?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "finance") {
            FinanceTopicCard(
                state = uiState.finance,
                onOpen = { onEvent(HomeEvent.OnOpenFinance(activity)) },
                onToggleReveal = {
                    onEvent(if (uiState.finance.isRevealed) HomeEvent.OnHideFinance else HomeEvent.OnRevealFinance(activity))
                },
                onAddExpense = { onEvent(HomeEvent.OnAddExpense(activity)) },
                onAddIncome = { onEvent(HomeEvent.OnAddIncome(activity)) },
                onRetry = { onEvent(HomeEvent.OnRefresh) },
            )
        }
        item(key = "smart_tags") {
            SmartTagsTopicCard(
                state = uiState.smartTags,
                onOpenQrCodes = { onEvent(HomeEvent.OnOpenQrCodes(activity)) },
                onOpenNfc = { onEvent(HomeEvent.OnOpenNfc(activity)) },
                onRetry = { onEvent(HomeEvent.OnRefresh) },
            )
        }
        item(key = "workout") {
            WorkoutTopicCard(
                state = uiState.workout,
                onOpen = { onEvent(HomeEvent.OnOpenWorkoutHub) },
                onOpenTodayWorkout = { onEvent(HomeEvent.OnOpenTodayWorkout(it)) },
                onOpenDiet = { onEvent(HomeEvent.OnOpenDiet) },
                onRetry = { onEvent(HomeEvent.OnRefresh) },
            )
        }
    }
}
