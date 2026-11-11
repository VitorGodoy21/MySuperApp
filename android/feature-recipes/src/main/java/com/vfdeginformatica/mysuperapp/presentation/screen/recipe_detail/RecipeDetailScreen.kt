package com.vfdeginformatica.mysuperapp.presentation.screen.recipe_detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vfdeginformatica.mysuperapp.domain.model.*
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_detail.contract.*

@Composable
fun RecipeDetailScreen(
    state: RecipeDetailUiState,
    onEvent: (RecipeDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
    photo: @Composable (Recipe, Modifier) -> Unit,
) {
    val draft = state.draft
    val enabled = state.canEdit
    fun change(recipe: Recipe) = onEvent(RecipeDetailEvent.DraftChanged(recipe))
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onEvent(RecipeDetailEvent.PhotoSelected(it.toString())) }
    }
    if (state.confirmLeave) AlertDialog(
        onDismissRequest = { onEvent(RecipeDetailEvent.Stay) },
        title = { Text("Descartar alterações?") }, text = { Text("As alterações não salvas serão perdidas.") },
        confirmButton = { TextButton(onClick = { onEvent(RecipeDetailEvent.Discard) }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { onEvent(RecipeDetailEvent.Stay) }) { Text("Continuar editando") } },
    )
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isLoading || state.isSaving) CircularProgressIndicator()
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            if (state.original == null && !state.accountChanged) {
                TextButton(onClick = { onEvent(RecipeDetailEvent.Reload) }) { Text("Tentar novamente") }
            }
        }
        if (state.original == null) return@Column
        RecipeTextField("Nome *", draft.name, enabled, singleLine = true) { change(draft.copy(name = it.take(200))) }
        RecipeTextField("Descrição", draft.description, enabled) { change(draft.copy(description = it.take(4000))) }
        Text("Foto", style = MaterialTheme.typography.titleMedium)
        val photoModifier = Modifier.fillMaxWidth().height(220.dp)
        when {
            state.photoUri != null -> AsyncImage(
                model = state.photoUri, contentDescription = "Foto selecionada", contentScale = ContentScale.Crop,
                modifier = photoModifier,
            )
            !state.removePhoto && draft.imagePath != null -> photo(draft, photoModifier)
        }
        Row {
            TextButton(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = enabled,
            ) { Text("Escolher foto") }
            if (state.photoUri != null || (!state.removePhoto && draft.imagePath != null)) {
                TextButton(onClick = { onEvent(RecipeDetailEvent.RemovePhoto) }, enabled = enabled) { Text("Remover foto") }
            }
        }
        Text("Ingredientes *", style = MaterialTheme.typography.titleMedium)
        draft.ingredients.forEachIndexed { index, ingredient ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorRowControls(index, draft.ingredients.size, enabled,
                        onMove = { target -> change(draft.copy(ingredients = draft.ingredients.moved(index, target))) },
                        onRemove = { change(draft.copy(ingredients = draft.ingredients.filterIndexed { i, _ -> i != index })) },
                    )
                    RecipeTextField("Ingrediente", ingredient.name, enabled, singleLine = true) { value ->
                        change(draft.copy(ingredients = draft.ingredients.mapIndexed { i, item -> if (i == index) item.copy(name = value.take(200)) else item }))
                    }
                    RecipeTextField("Quantidade (ex.: 2 xícaras)", ingredient.quantity, enabled, singleLine = true) { value ->
                        change(draft.copy(ingredients = draft.ingredients.mapIndexed { i, item -> if (i == index) item.copy(quantity = value.take(200)) else item }))
                    }
                }
            }
        }
        TextButton(
            onClick = { change(draft.copy(ingredients = draft.ingredients + RecipeIngredient())) },
            enabled = enabled && draft.ingredients.size < 100,
        ) { Text("Adicionar ingrediente") }
        Text("Modo de preparo *", style = MaterialTheme.typography.titleMedium)
        draft.steps.forEachIndexed { index, step ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    EditorRowControls(index, draft.steps.size, enabled,
                        onMove = { target -> change(draft.copy(steps = draft.steps.moved(index, target))) },
                        onRemove = { change(draft.copy(steps = draft.steps.filterIndexed { i, _ -> i != index })) },
                    )
                    RecipeTextField("Passo ${index + 1}", step, enabled) { value ->
                        change(draft.copy(steps = draft.steps.mapIndexed { i, item -> if (i == index) value.take(4000) else item }))
                    }
                }
            }
        }
        TextButton(onClick = { change(draft.copy(steps = draft.steps + "")) }, enabled = enabled && draft.steps.size < 100) {
            Text("Adicionar passo")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = draft.prepMinutes?.toString().orEmpty(),
                onValueChange = { change(draft.copy(prepMinutes = it.filter(Char::isDigit).take(6).toIntOrNull())) },
                label = { Text("Preparo (min)") }, enabled = enabled, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = draft.cookMinutes?.toString().orEmpty(),
                onValueChange = { change(draft.copy(cookMinutes = it.filter(Char::isDigit).take(6).toIntOrNull())) },
                label = { Text("Cozimento (min)") }, enabled = enabled, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
            )
        }
        RecipeTextField("Rendimento (ex.: 4 porções)", draft.yield, enabled, singleLine = true) { change(draft.copy(yield = it.take(200))) }
        RecipeChoice("Categoria", draft.category?.label, RecipeCategory.entries.map { it.label }, enabled) { index ->
            change(draft.copy(category = index?.let { RecipeCategory.entries[it] }))
        }
        RecipeChoice("Dificuldade", draft.difficulty?.label, RecipeDifficulty.entries.map { it.label }, enabled) { index ->
            change(draft.copy(difficulty = index?.let { RecipeDifficulty.entries[it] }))
        }
        RecipeTextField("Observações", draft.notes, enabled) { change(draft.copy(notes = it.take(4000))) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = draft.isFavorite, enabled = enabled, onCheckedChange = { change(draft.copy(isFavorite = it)) })
            Text("Receita favorita")
        }
        Button(onClick = { onEvent(RecipeDetailEvent.Save) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.isSaving) "Salvando..." else "Salvar receita")
        }
    }
}

@Composable
private fun RecipeTextField(
    label: String, value: String, enabled: Boolean, singleLine: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label) }, enabled = enabled,
        singleLine = singleLine, minLines = if (singleLine) 1 else 2, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RecipeChoice(label: String, selected: String?, options: List<String>, enabled: Boolean, onSelect: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text(selected ?: "Não informado") }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("Não informado") }, onClick = { onSelect(null); expanded = false })
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(index); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun EditorRowControls(index: Int, size: Int, enabled: Boolean, onMove: (Int) -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${index + 1}", modifier = Modifier.weight(1f))
        IconButton(onClick = { onMove(index - 1) }, enabled = enabled && index > 0) { Icon(Icons.Default.ArrowUpward, "Mover para cima") }
        IconButton(onClick = { onMove(index + 1) }, enabled = enabled && index < size - 1) { Icon(Icons.Default.ArrowDownward, "Mover para baixo") }
        IconButton(onClick = onRemove, enabled = enabled) { Icon(Icons.Default.Delete, "Remover") }
    }
}

internal fun <T> List<T>.moved(from: Int, to: Int): List<T> =
    toMutableList().apply { add(to, removeAt(from)) }
