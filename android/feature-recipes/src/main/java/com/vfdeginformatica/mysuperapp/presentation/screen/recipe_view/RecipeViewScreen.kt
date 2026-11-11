package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.Recipe
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_view.contract.*

@Composable
fun RecipeViewScreen(
    state: RecipeViewUiState,
    onEvent: (RecipeViewEvent) -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    photo: @Composable (Recipe, Modifier) -> Unit,
) {
    if (state.confirmDelete) AlertDialog(
        onDismissRequest = { onEvent(RecipeViewEvent.DismissDelete) },
        title = { Text("Excluir receita?") }, text = { Text("Esta ação não pode ser desfeita.") },
        confirmButton = { TextButton(onClick = { onEvent(RecipeViewEvent.ConfirmDelete) }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { onEvent(RecipeViewEvent.DismissDelete) }) { Text("Cancelar") } },
    )
    LazyColumn(
        modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isLoading || state.isBusy) item { CircularProgressIndicator() }
        state.error?.let { error ->
            item {
                Text(error, color = MaterialTheme.colorScheme.error)
                if (!state.accountChanged) TextButton(onClick = { onEvent(RecipeViewEvent.Reload) }) { Text("Tentar novamente") }
            }
        }
        val recipe = state.recipe ?: return@LazyColumn
        item {
            Text(recipe.name, style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onEdit(recipe.id) }, enabled = !state.isBusy) { Text("Editar") }
                TextButton(onClick = { onEvent(RecipeViewEvent.AskDelete) }, enabled = !state.isBusy) { Text("Excluir") }
                TextButton(onClick = { onEvent(RecipeViewEvent.ToggleFavorite) }, enabled = !state.isBusy) {
                    Text(if (recipe.isFavorite) "★ Favorita" else "☆ Favoritar")
                }
            }
        }
        if (recipe.imagePath != null) item { photo(recipe, Modifier.fillMaxWidth().height(240.dp)) }
        if (recipe.description.isNotBlank()) item { Text(recipe.description) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                recipe.category?.let { Text("Categoria: ${it.label}") }
                recipe.difficulty?.let { Text("Dificuldade: ${it.label}") }
                recipe.prepMinutes?.let { Text("Preparo: $it min") }
                recipe.cookMinutes?.let { Text("Cozimento: $it min") }
                recipe.totalMinutes?.let { Text("Tempo total: $it min") }
                if (recipe.yield.isNotBlank()) Text("Rendimento: ${recipe.yield}")
            }
        }
        item { Text("Ingredientes", style = MaterialTheme.typography.titleLarge) }
        itemsIndexed(recipe.ingredients) { _, ingredient -> Text("• ${ingredient.quantity} — ${ingredient.name}") }
        item { Text("Modo de preparo", style = MaterialTheme.typography.titleLarge) }
        itemsIndexed(recipe.steps) { index, step -> Text("${index + 1}. $step") }
        if (recipe.notes.isNotBlank()) item {
            Text("Observações", style = MaterialTheme.typography.titleLarge)
            Text(recipe.notes)
        }
    }
}
