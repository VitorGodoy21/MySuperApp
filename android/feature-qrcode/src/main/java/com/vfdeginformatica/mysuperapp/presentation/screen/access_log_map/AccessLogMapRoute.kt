package com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.accessLogMapRoute(navController: NavController) {
    composable(
        route = "access_log_map/{qrCodeId}"
    ) { backStackEntry ->
        val qrCodeId = backStackEntry.arguments?.getString("qrCodeId") ?: ""
        val viewModel: AccessLogMapViewModel = hiltViewModel()
        val uiState = viewModel.uiState.collectAsState().value
        val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

        Scaffold(
        ) { innerPadding ->
            AccessLogMapScreen(
                uiState = uiState,
                onEvent = { event -> viewModel.onEvent(event) },
                qrCodeId = qrCodeId,
                contentPadding = innerPadding
            )
        }
    }
}

fun NavController.navigateToAccessLogMap(qrCodeId: String) {
    navigate("access_log_map/$qrCodeId")
}
