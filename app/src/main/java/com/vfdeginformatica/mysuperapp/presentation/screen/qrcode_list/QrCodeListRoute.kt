package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.AppDrawerMenuRoute
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEffect

@Composable
fun QrCodeListRoute(
    viewModel: QrCodeListViewModel = hiltViewModel(),
    navController: NavHostController,
    onNavigateQrCode: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QrCodeListEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
                is QrCodeListEffect.NavigateToQrCode -> onNavigateQrCode.invoke(effect.id)
            }
        }
    }

    AppDrawerMenuRoute(
        navController = navController
    ) { onToggleDrawer ->
        AppScaffold(
            title = "QR Codes",
            canNavigateUp = true,
            onNavigateUp = { onToggleDrawer.invoke() },
            snackBarHostState = snackBarHost
        ) { padding ->
            QrCodeListScreen(
                uiState = state,
                onEvent = viewModel::onEvent,
                modifier = Modifier.padding(padding),
                padding
            )
        }
    }

}
