package com.vfdeginformatica.qrcodemanager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.presentation.common.ui.theme.MySuperAppTheme
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.accessLogMapRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.login.LoginRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.muralCommentsRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.QrCodeRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.QrCodeViewModel
import com.vfdeginformatica.qrcodemanager.profile.ProfileRoute

@Composable
fun QrCodeManagerNavGraph(
    sessionViewModel: QrCodeManagerSessionViewModel = hiltViewModel()
) {
    val state by sessionViewModel.uiState.collectAsStateWithLifecycle()

    MySuperAppTheme {
        val navController = rememberNavController()

        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = QrCodeManagerScreen.Splash.route
        ) {
                // ── Splash ──────────────────────────────────────────────────
                composable(route = QrCodeManagerScreen.Splash.route) {
                    QrCodeManagerSplashRoute(
                        status = state,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(QrCodeManagerScreen.Splash.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // ── Login ────────────────────────────────────────────────────
                composable(route = QrCodeManagerScreen.Login.route) {
                    LoginRoute(
                        onNavigateHome = {
                            navController.navigate(QrCodeManagerScreen.QrCodeList.route) {
                                popUpTo(QrCodeManagerScreen.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // ── QR Code list ─────────────────────────────────────────────
                composable(route = QrCodeManagerScreen.QrCodeList.route) {
                    QrCodeManagerListRoute(
                        navController = navController,
                        onNavigateQrCode = { qrCode ->
                            navController.navigate(
                                QrCodeManagerScreen.QrCodeDetail.createRoute(qrCode)
                            )
                        },
                        onLogout = {
                            navController.navigate(QrCodeManagerScreen.Login.route) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // ── QR Code detail / edit ────────────────────────────────────
                composable(
                    route = QrCodeManagerScreen.QrCodeDetail.route,
                    arguments = listOf(
                        navArgument(QrCodeManagerScreen.QrCodeDetail.QR_CODE_DATA) {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val json =
                        entry.arguments!!.getString(QrCodeManagerScreen.QrCodeDetail.QR_CODE_DATA)!!
                    val qrCode = Gson().fromJson(json, QrCode::class.java)
                    val viewModel: QrCodeViewModel = hiltViewModel(entry)
                    QrCodeRoute(
                        viewModel = viewModel,
                        navController = navController,
                        qrCode = qrCode
                    )
                }

                // ── Access log map ───────────────────────────────────────────
                accessLogMapRoute(navController)

                // ── Mural comments ───────────────────────────────────────────
                muralCommentsRoute(navController)

            // ── Profile ──────────────────────────────────────────────────
            composable(route = QrCodeManagerScreen.Profile.route) {
                ProfileRoute(navController = navController)
            }
            }
    }
}

