package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Anchor
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vfdeginformatica.mysuperapp.domain.model.HomeMenuItem
import com.vfdeginformatica.mysuperapp.presentation.screen.home.components.HomeMenuCardItem
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeUiState

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
    innerPadding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.items) { item ->
                HomeMenuCardItem(
                    item = item,
                    onItemClick = {
                        onEvent(HomeEvent.OnMenuItemNavigate(item.route))
                    })
            }
        }
        if (uiState.errorMessage.isNotBlank()) {
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .align(Alignment.Center)
            )
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        uiState = HomeUiState(
            isLoading = false,
            errorMessage = "",
            items = listOf(
                HomeMenuItem(
                    title = "Finanças",
                    icon = Icons.Default.AllInclusive,
                    route = "teste",
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    content = {}
                ),
                HomeMenuItem(
                    title = "Academia",
                    icon = Icons.Default.Anchor,
                    route = "teste",
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    content = {}
                ),
            )
        ),
        onEvent = {},
        innerPadding = PaddingValues(0.dp)
    )
}