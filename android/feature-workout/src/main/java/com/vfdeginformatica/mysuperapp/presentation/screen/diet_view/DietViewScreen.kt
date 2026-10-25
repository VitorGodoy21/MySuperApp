package com.vfdeginformatica.mysuperapp.presentation.screen.diet_view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_view.contract.DietViewUiState

@Composable
fun DietViewScreen(state: DietViewUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            state.isLoading -> item { CircularProgressIndicator() }
            state.error != null -> item {
                Text(state.error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Tentar novamente") }
            }
            state.diet == null -> item {
                Text("Nenhuma dieta cadastrada. Cadastre em Menu → Academia → Dieta.")
            }
            else -> {
                if (state.diet.notes.isNotBlank()) item { Text(state.diet.notes) }
                if (state.diet.meals.isEmpty()) item { Text("Nenhuma refeição cadastrada.") }
                items(state.diet.meals, key = { it.id }) { meal ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(meal.name, style = MaterialTheme.typography.titleMedium)
                            if (meal.time.isNotBlank()) Text(meal.time, color = MaterialTheme.colorScheme.primary)
                            if (meal.notes.isNotBlank()) Text(meal.notes)
                            if (meal.foods.isEmpty()) Text("Nenhum alimento cadastrado.")
                            meal.foods.forEach { food ->
                                Text("${food.name} — ${food.quantity}")
                            }
                        }
                    }
                }
            }
        }
    }
}
