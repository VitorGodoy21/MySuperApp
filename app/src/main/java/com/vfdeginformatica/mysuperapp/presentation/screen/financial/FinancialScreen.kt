package com.vfdeginformatica.mysuperapp.presentation.screen.financial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialUiState

@Composable
fun FinancialScreen(
    uiState: FinancialUiState,
    onEvent: (FinancialEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Financial Screen")
    }
}

@Preview(showBackground = true)
@Composable
fun FinancialScreenPreview() {
    FinancialScreen(
        uiState = FinancialUiState(),
        onEvent = {}
    )
}