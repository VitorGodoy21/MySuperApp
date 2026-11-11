package com.vfdeginformatica.mysuperapp.presentation.screen.spotify_connection

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.spotify_connection.contract.SpotifyConnectionEffect

@Composable
fun SpotifyConnectionRoute(
    onNavigateUp: () -> Unit,
    viewModel: SpotifyConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackBar = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SpotifyConnectionEffect.ShowToast ->
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()

                is SpotifyConnectionEffect.OpenUrl -> try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, "Nenhum navegador disponível", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AppScaffold(
        title = "Spotify",
        canNavigateUp = true,
        customizedNavigateUpIcon = Icons.AutoMirrored.Filled.ArrowBack,
        onNavigateUp = onNavigateUp,
        snackBarHostState = snackBar
    ) { padding ->
        SpotifyConnectionScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }
}
