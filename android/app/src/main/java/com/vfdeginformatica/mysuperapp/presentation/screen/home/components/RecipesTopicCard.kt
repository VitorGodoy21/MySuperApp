package com.vfdeginformatica.mysuperapp.presentation.screen.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.runtime.Composable

@Composable
fun RecipesTopicCard(onOpen: () -> Unit, onAdd: () -> Unit) {
    val colors = HomeColors.recipes()
    HomeTopicCard(
        title = "Receitas culinárias",
        subtitle = "Seu caderno de receitas",
        icon = Icons.Default.MenuBook,
        colors = colors,
        onHeaderClick = onOpen,
    ) {
        HomeShortcutChip("Minhas receitas", Icons.Default.MenuBook, colors, onOpen)
        HomeShortcutChip("Adicionar receita", Icons.Default.Add, colors, onAdd)
    }
}
