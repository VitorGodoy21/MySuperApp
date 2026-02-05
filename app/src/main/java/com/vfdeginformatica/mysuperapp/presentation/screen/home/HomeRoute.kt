package com.vfdeginformatica.mysuperapp.presentation.screen.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.AppDrawerMenuRoute
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.home.contract.HomeEffect

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavHostController,
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
                is HomeEffect.NavigateToMenuItem -> onNavigate(effect.route)
            }
        }
    }

    AppDrawerMenuRoute(
        navController = navController
    ) { onToggleDrawer ->
        AppScaffold(
            title = "My Super App",
            canNavigateUp = true,
            customizedNavigateUpIcon = Icons.Default.Menu,
            onNavigateUp = { onToggleDrawer.invoke() },
            toolbarActions = {},
            snackBarHostState = snackBarHost
        ) { padding ->
            HomeScreen(
                uiState = state,
                onEvent = viewModel::onEvent,
                innerPadding = padding
            )
        }
    }
}