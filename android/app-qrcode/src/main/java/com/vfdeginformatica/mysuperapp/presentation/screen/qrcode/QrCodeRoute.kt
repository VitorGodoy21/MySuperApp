package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEvent

@Composable
fun QrCodeRoute(
    viewModel: QrCodeViewModel,
    navController: NavHostController,
    qrCode: QrCode
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(qrCode) {
        viewModel.onEvent(QrCodeEvent.OnQrCodeLoaded(qrCode))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QrCodeEffect.ShowToast -> {
                    snackBarHost.showSnackbar(effect.message)
                }
            }
        }
    }

    AppScaffold(
        title = "Editar QR Code",
        canNavigateUp = true,
        onNavigateUp = { navController.popBackStack() },
        snackBarHostState = snackBarHost
    ) { padding ->
        QrCodeScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            onViewAccessLogs = { qrCodeId ->
                navController.navigate("access_log_map/$qrCodeId")
            },
            onViewMural = { qrCodeId, identifier ->
                navController.navigate(Screen.MuralCommentsScreen.createRoute(qrCodeId, identifier))
            },
            modifier = Modifier.padding(padding)
        )
    }
}


