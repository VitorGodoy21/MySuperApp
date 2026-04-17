package com.vfdeginformatica.qrcodemanager

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import com.vfdeginformatica.qrcodemanager.drawer.QrCodeManagerDrawerScreen
import com.vfdeginformatica.qrcodemanager.drawer.QrCodeManagerDrawerViewModel

/**
 * Standalone QR Code list route for the QR Code Manager app.
 * Includes a logout button in the toolbar.
 */
@Composable
fun QrCodeManagerListRoute(
    viewModel: QrCodeListViewModel = hiltViewModel(),
    logoutViewModel: QrCodeManagerLogoutViewModel = hiltViewModel(),
    drawerViewModel: QrCodeManagerDrawerViewModel = hiltViewModel(),
    navController: NavHostController,
    onNavigateQrCode: (QrCode) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState by drawerViewModel.uiState.collectAsStateWithLifecycle()
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
                is QrCodeManagerLogoutViewModel.LogoutEffect.NavigateToLogin -> onLogout()
                is QrCodeManagerLogoutViewModel.LogoutEffect.ShowError ->
                    snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    QrCodeManagerDrawerScreen(
        userName = drawerState.userName,
        userEmail = drawerState.userEmail,
        isOpen = drawerState.isOpen,
        onToggle = { drawerViewModel.toggleDrawer() },
        onProfile = {
            navController.navigate(QrCodeManagerScreen.Profile.route)
        },
        onNotifications = {
            // TODO: navegar para tela de notificações quando implementada
        },
        onLogout = { logoutViewModel.logout() }
    ) {
        AppScaffold(
            title = "QR Codes",
            canNavigateUp = true,
            customizedNavigateUpIcon = Icons.Default.Menu,
            onNavigateUp = { drawerViewModel.toggleDrawer() },
            snackBarHostState = snackBarHost,
            toolbarActions = {}
        ) { padding ->
            QrCodeListScreen(
                uiState = state,
                onEvent = viewModel::onEvent,
                modifier = Modifier.padding(padding),
                innerPadding = padding
            )
        }
    }
}
