package com.vfdeginformatica.qrcodemanager

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
 * Includes a logout button in the toolbar.
 */
@Composable
fun QrCodeManagerListRoute(
    viewModel: QrCodeListViewModel = hiltViewModel(),
    logoutViewModel: QrCodeManagerLogoutViewModel = hiltViewModel(),
    navController: NavHostController,
    onNavigateQrCode: (QrCode) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoggingOut by logoutViewModel.isLoading.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }

    // Reload list whenever this screen becomes active
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry?.destination?.route) {
        if (currentEntry?.destination?.route == QrCodeManagerScreen.QrCodeList.route) {
            viewModel.refresh()
        }
    }

    // QR code list effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QrCodeListEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
                is QrCodeListEffect.NavigateToQrCode -> onNavigateQrCode(effect.qrCode)
            }
        }
    }

    // Logout effects
    LaunchedEffect(Unit) {
        logoutViewModel.effect.collect { effect ->
            when (effect) {
                is QrCodeManagerLogoutViewModel.LogoutEffect.NavigateToLogin ->
                    onLogout()

                is QrCodeManagerLogoutViewModel.LogoutEffect.ShowError ->
                    snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = "QR Codes",
        canNavigateUp = false,
        snackBarHostState = snackBarHost,
        toolbarActions = {
            IconButton(
                onClick = { logoutViewModel.logout() },
                enabled = !isLoggingOut
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sair"
                    )
                }
            }
        }
    ) { padding ->
        QrCodeListScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding),
            innerPadding = padding
        )
    }
}
