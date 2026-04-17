package com.vfdeginformatica.qrcodemanager.notifications

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold

@Composable
fun NotificationsRoute(
    navController: NavHostController,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NotificationsEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AppScaffold(
        title = "Notificações",
        canNavigateUp = true,
        customizedNavigateUpIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onNavigateUp = { navController.popBackStack() },
        snackBarHostState = snackBar
    ) { padding ->
        NotificationsScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}

