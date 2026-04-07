package com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments

import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.presentation.components.toolbar.AppScaffold
import com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.contract.MuralCommentsEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.contract.MuralCommentsEvent

fun NavGraphBuilder.muralCommentsRoute(navController: NavController) {
    composable(
        route = Screen.MuralCommentsScreen.route,
        arguments = listOf(
            navArgument(Screen.MuralCommentsScreen.QR_CODE_ID) { type = NavType.StringType },
            navArgument(Screen.MuralCommentsScreen.QR_CODE_IDENTIFIER) {
                type = NavType.StringType
                defaultValue = ""
            }
        )
    ) { backStackEntry ->
        val qrCodeId =
            backStackEntry.arguments?.getString(Screen.MuralCommentsScreen.QR_CODE_ID) ?: ""
        val identifier =
            backStackEntry.arguments?.getString(Screen.MuralCommentsScreen.QR_CODE_IDENTIFIER) ?: ""

        val viewModel: MuralCommentsViewModel = hiltViewModel()
        val uiState = viewModel.uiState.collectAsState().value
        val snackBarHost: SnackbarHostState = remember { SnackbarHostState() }
        val context = LocalContext.current

        LaunchedEffect(qrCodeId) {
            viewModel.onEvent(MuralCommentsEvent.OnLoadComments(qrCodeId))
        }

        LaunchedEffect(Unit) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is MuralCommentsEffect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        val title = if (identifier.isNotEmpty()) "Mural: $identifier" else "Mural de Comentários"

        AppScaffold(
            title = title,
            canNavigateUp = true,
            onNavigateUp = { navController.popBackStack() },
            snackBarHostState = snackBarHost
        ) { innerPadding ->
            MuralCommentsScreen(
                qrCodeId = qrCodeId,
                uiState = uiState,
                onEvent = { event -> viewModel.onEvent(event) },
                contentPadding = innerPadding
            )
        }
    }
}

