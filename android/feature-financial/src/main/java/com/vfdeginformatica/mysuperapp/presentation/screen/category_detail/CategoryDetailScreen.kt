package com.vfdeginformatica.mysuperapp.presentation.screen.category_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.presentation.screen.category_detail.contract.CategoryDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.category_detail.contract.CategoryDetailUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

@Composable
fun CategoryDetailScreen(
    uiState: CategoryDetailUiState,
    onEvent: (CategoryDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    if (uiState.showDeleteConfirmation) {
        DeleteCategoryDialog(
            title = uiState.title,
            onConfirm = { onEvent(CategoryDetailEvent.DeleteConfirmed) },
            onDismiss = { onEvent(CategoryDetailEvent.DeleteDismissed) },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Tipo", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.transactionType == TransactionType.EXPENSE,
                onClick = { onEvent(CategoryDetailEvent.TypeChanged(TransactionType.EXPENSE)) },
                label = { Text("Despesa") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = uiState.transactionType == TransactionType.INCOME,
                onClick = { onEvent(CategoryDetailEvent.TypeChanged(TransactionType.INCOME)) },
                label = { Text("Receita") },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = uiState.title,
            onValueChange = { onEvent(CategoryDetailEvent.TitleChanged(it)) },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = { onEvent(CategoryDetailEvent.DescriptionChanged(it)) },
            label = { Text("Descrição (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )

        Button(
            onClick = { onEvent(CategoryDetailEvent.SaveClicked) },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.isSubmitEnabled && !uiState.isBusy,
        ) {
            Text(
                when {
                    uiState.isSaving -> "Salvando..."
                    uiState.isEditing -> "Salvar alterações"
                    else -> "Criar categoria"
                }
            )
        }

        if (uiState.isEditing) {
            HorizontalDivider()

            Text(
                text = "Ao excluir, esta categoria é removida de todas as transações que a utilizam.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = { onEvent(CategoryDetailEvent.DeleteClicked) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isBusy,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (uiState.isDeleting) "Excluindo..." else "Excluir categoria")
            }
        }
    }
}

@Composable
private fun DeleteCategoryDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir categoria?") },
        text = {
            Text(
                "A categoria \"$title\" será excluída e removida de todas as transações " +
                        "que a utilizam. Esta ação não pode ser desfeita."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryDetailScreenPreview() {
    CategoryDetailScreen(
        uiState = CategoryDetailUiState(
            categoryId = "1",
            title = "Mercado",
            description = "Compras do mês",
            transactionType = TransactionType.EXPENSE,
            isSubmitEnabled = true
        ),
        onEvent = {}
    )
}
