package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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
    val flags = uiState.flags
    if (!flags.hasAnyEnabled) {
        HomeEmptyState(modifier = Modifier.fillMaxSize().padding(innerPadding))
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (flags.finance) item(key = "finance") {
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
        if (flags.isSmartTagsEnabled) item(key = "smart_tags") {
            SmartTagsTopicCard(
                state = uiState.smartTags,
                showQrCode = flags.isQrCodeEnabled,
                showNfc = flags.isNfcEnabled,
                onOpenQrCodes = { onEvent(HomeEvent.OnOpenQrCodes(activity)) },
                onOpenNfc = { onEvent(HomeEvent.OnOpenNfc(activity)) },
                onRetry = { onEvent(HomeEvent.OnRefresh) },
            )
        }
        if (flags.workout) item(key = "workout") {
            WorkoutTopicCard(
                state = uiState.workout,
                showDiet = flags.isDietEnabled,
                onOpen = { onEvent(HomeEvent.OnOpenWorkoutHub) },
                onOpenTodayWorkout = { onEvent(HomeEvent.OnOpenTodayWorkout(it)) },
                onOpenDiet = { onEvent(HomeEvent.OnOpenDiet) },
                onRetry = { onEvent(HomeEvent.OnRefresh) },
            )
        }
    }
}

@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Nenhuma função ativa",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Ative as funções do app no painel web (Configurações) e abra o app novamente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
