package com.vfdeginformatica.qrcodemanager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
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
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.NfcRoute
import com.vfdeginformatica.mysuperapp.notification.QrCodeDeepLinkEffect
import com.vfdeginformatica.mysuperapp.notification.QrCodeDeepLinkViewModel
import com.vfdeginformatica.mysuperapp.presentation.screen.notifications.NotificationsRoute
import com.vfdeginformatica.qrcodemanager.profile.ProfileRoute

@Composable
fun QrCodeManagerNavGraph(
    sessionViewModel: QrCodeManagerSessionViewModel = hiltViewModel(),
    deepLinkViewModel: QrCodeDeepLinkViewModel = hiltViewModel()
) {
    val state by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val pendingQrCodeId by deepLinkViewModel.pendingQrCodeId.collectAsStateWithLifecycle()
    val context = LocalContext.current

    MySuperAppTheme {
        val navController = rememberNavController()

        // Deep link vindo de notificação: só resolve quando a sessão estiver logada.
        LaunchedEffect(pendingQrCodeId, state.isLoggedIn) {
            if (pendingQrCodeId != null && state.isLoggedIn) deepLinkViewModel.consumePending()
        }

        LaunchedEffect(Unit) {
            deepLinkViewModel.effect.collect { effect ->
                when (effect) {
                    is QrCodeDeepLinkEffect.OpenQrCode -> navController.navigate(
                        QrCodeManagerScreen.QrCodeDetail.createRoute(effect.qrCode)
                    ) { launchSingleTop = true }

                    is QrCodeDeepLinkEffect.NotFound ->
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

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

            // ── Notifications ─────────────────────────────────────────────
            composable(route = QrCodeManagerScreen.Notifications.route) {
                NotificationsRoute(onNavigateUp = { navController.popBackStack() })
            }

            // ── NFC ──────────────────────────────────────────────────────
            composable(route = QrCodeManagerScreen.Nfc.route) {
                NfcRoute(onNavigateUp = { navController.navigateUp() })
            }
        }
    }
}
