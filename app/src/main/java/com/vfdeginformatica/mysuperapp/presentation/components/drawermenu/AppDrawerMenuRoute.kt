package com.vfdeginformatica.mysuperapp.presentation.components.drawermenu

import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract.AppDrawerMenuEffect
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract.AppDrawerMenuEvent

@Composable
fun AppDrawerMenuRoute(
    viewModel: AppDrawerMenuViewModel = hiltViewModel(),
    navController: NavHostController,
    content: @Composable (
        onToggleDrawer: () -> Unit
    ) -> Unit
) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AppDrawerMenuEffect.Navigate -> {
                    navController.navigate(effect.route) {
                        popUpTo(effect.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }

                AppDrawerMenuEffect.NavigateToLogin -> {
                    navController.navigate(Screen.LoginScreen.route) {
                        popUpTo(Screen.LoginScreen.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }

                is AppDrawerMenuEffect.ShowToastMessage -> {
                    Toast.makeText(context, effect.msg, Toast.LENGTH_SHORT).show()
                }

                is AppDrawerMenuEffect.ShowSnackBarMessage -> {
                    snackBarHost.showSnackbar(effect.msg)
                }

                AppDrawerMenuEffect.NavigateToHome -> {
                    navController.navigate(Screen.HomeScreen.route) {
                        popUpTo(Screen.HomeScreen.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    AppDrawerMenuScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onLogout = { viewModel.logout() },
        onHome = { viewModel.goHome() },
        onFinancialManagement = { viewModel.navigate(Screen.FinancialScreen.route) },
        onSettings = {},
        onResume = { viewModel.onEvent(AppDrawerMenuEvent.OnResume) }
    ) {
        content {
            if (state.isOpen) {
                viewModel.onEvent(AppDrawerMenuEvent.Close)
            } else {
                viewModel.onEvent(AppDrawerMenuEvent.Open)
            }
        }
    }

}