package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.AppDrawerMenuRoute
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEvent

@Composable
fun QrCodeListRoute(
    viewModel: QrCodeListViewModel = hiltViewModel(),
    navController: NavHostController,
    onNavigateQrCode: (QrCode) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Recarrega os dados sempre que esta tela se tornar a tela ativa
    // (carga inicial + retorno de outra tela como a de edição)
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentEntry?.destination?.route) {
        if (currentEntry?.destination?.route == Screen.QrCodeListScreen.route) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QrCodeListEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
                is QrCodeListEffect.NavigateToQrCode -> {
                    navController.navigate(Screen.QrCodeScreen.createRoute(effect.qrCode))
                }
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
            snackBarHostState = snackBarHost,
            floatingActionButtonOnClick = { showCreateDialog = true },
            floatingActionButtonLabel = "Novo QR Code"
        ) { padding ->
            QrCodeListScreen(
                uiState = state,
                onEvent = viewModel::onEvent,
                modifier = Modifier.padding(padding),
                innerPadding = padding,
                showCreateDialog = showCreateDialog,
                onDismissCreateDialog = { showCreateDialog = false },
                onConfirmCreateQrCode = { type, identifier ->
                    showCreateDialog = false
                    viewModel.onEvent(QrCodeListEvent.OnCreateQrCode(type, identifier))
                }
            )
        }
    }

}
