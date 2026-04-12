package com.vfdeginformatica.qrcodemanager

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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.QrCodeListScreen
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.QrCodeListViewModel
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEffect

/**
 * Standalone QR Code list route for the QR Code Manager app.
 * Mirrors QrCodeListRoute from :app but uses AppScaffold directly
 * (no navigation drawer — the manager app is single-purpose).
 */
@Composable
fun QrCodeManagerListRoute(
    viewModel: QrCodeListViewModel = hiltViewModel(),
    navController: NavHostController,
    onNavigateQrCode: (QrCode) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry?.destination?.route) {
        if (currentEntry?.destination?.route == QrCodeManagerScreen.QrCodeList.route) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QrCodeListEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
                is QrCodeListEffect.NavigateToQrCode -> onNavigateQrCode(effect.qrCode)
            }
        }
    }

    AppScaffold(
        title = "QR Codes",
        canNavigateUp = false,
        snackBarHostState = snackBarHost
    ) { padding ->
        QrCodeListScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding),
            innerPadding = padding
        )
    }
}

