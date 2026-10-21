package com.vfdeginformatica.mysuperapp.presentation.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.settings.contract.SettingsEffect

@Composable
fun SettingsRoute(
    onNavigateUp: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val snackBar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.Navigate -> onNavigate(effect.route)
            }
        }
    }

    AppScaffold(
        title = "Configurações",
        canNavigateUp = true,
        customizedNavigateUpIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBar
    ) { padding ->
        SettingsScreen(
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}
