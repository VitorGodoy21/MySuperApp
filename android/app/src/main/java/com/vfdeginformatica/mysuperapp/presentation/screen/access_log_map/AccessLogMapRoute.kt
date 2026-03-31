package com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map

import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.accessLogMapRoute() {
    composable(
        route = "access_log_map/{qrCodeId}"
    ) { backStackEntry ->
        val qrCodeId = backStackEntry.arguments?.getString("qrCodeId") ?: ""
        val viewModel: AccessLogMapViewModel = hiltViewModel()
        val uiState = viewModel.uiState.collectAsState().value

        AccessLogMapScreen(
            uiState = uiState,
            onEvent = { event -> viewModel.onEvent(event) },
            qrCodeId = qrCodeId
        )
    }
}

fun NavController.navigateToAccessLogMap(qrCodeId: String) {
    navigate("access_log_map/$qrCodeId")
}

