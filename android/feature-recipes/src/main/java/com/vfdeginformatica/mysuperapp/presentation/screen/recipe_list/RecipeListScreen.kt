package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.Recipe
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list.contract.RecipeListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list.contract.RecipeListUiState

@Composable
fun RecipeListScreen(
    state: RecipeListUiState,
    onEvent: (RecipeListEvent) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    photo: @Composable (Recipe, Modifier) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.query, onValueChange = { onEvent(RecipeListEvent.QueryChanged(it)) },
                label = { Text("Buscar por nome") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            FilterChip(
                selected = state.favoritesOnly, onClick = { onEvent(RecipeListEvent.FavoritesToggled) },
                label = { Text("Somente favoritas") },
            )
        }
        if (state.isLoading) item { CircularProgressIndicator() }
        state.error?.let { message ->
            item {
                Text(message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { onEvent(RecipeListEvent.Reload) }) { Text("Tentar novamente") }
            }
        }
        if (!state.isLoading && state.error == null && state.visibleRecipes.isEmpty()) item {
            Text(if (state.recipes.isEmpty()) "Você ainda não tem receitas. Toque em Adicionar receita."
            else "Nenhuma receita encontrada.")
        }
        items(state.visibleRecipes, key = { "${it.userId}/${it.id}" }) { recipe ->
            Card(onClick = { onOpen(recipe.id) }, modifier = Modifier.fillMaxWidth()) {
                if (recipe.imagePath != null) photo(recipe, Modifier.fillMaxWidth().height(160.dp))
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(recipe.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (recipe.isFavorite) Icon(Icons.Default.Star, "Favorita", tint = MaterialTheme.colorScheme.primary)
                    }
                    recipe.category?.let { Text(it.label, style = MaterialTheme.typography.labelMedium) }
                    val meta = listOfNotNull(recipe.totalMinutes?.let { "$it min" }, recipe.yield.takeIf { it.isNotBlank() })
                    if (meta.isNotEmpty()) Text(meta.joinToString(" • "), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
