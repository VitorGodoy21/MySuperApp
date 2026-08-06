package com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.common.getFragmentActivity
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.nfc.presentation.NfcForegroundDispatcher
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcEffect

/**
 * Tela hub de NFC (leitura, gravação e bloqueio de tag), compartilhada entre
 * o `:app` e o `:app-qrcode`.
 */
@Composable
fun NfcRoute(
    viewModel: NfcViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBarHost = remember { SnackbarHostState() }

    val context = LocalContext.current
    val activity = remember { context.getFragmentActivity() }
    val dispatcher = remember(activity) { activity?.let { NfcForegroundDispatcher(it) } }

    LaunchedEffect(dispatcher) {
        dispatcher?.let {
            viewModel.updateNfcHardwareStatus(it.isNfcSupported, it.isNfcEnabled)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, dispatcher) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> dispatcher?.enable()
                Lifecycle.Event.ON_PAUSE -> dispatcher?.disable()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            dispatcher?.disable()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NfcEffect.ShowToast -> snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    AppScaffold(
        title = "NFC",
        canNavigateUp = true,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBarHost
    ) { padding ->
        NfcScreen(
            uiState = state,
            onEvent = viewModel::onEvent,
            innerPadding = padding
        )
    }
}
