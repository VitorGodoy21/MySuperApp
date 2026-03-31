package com.vfdeginformatica.mysuperapp.presentation.my_super_app

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.presentation.common.getFragmentActivity
import com.vfdeginformatica.mysuperapp.presentation.common.ui.theme.MySuperAppTheme
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.accessLogMapRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.FinancialRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.home.HomeRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.login.LoginRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.NewTransactionRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.QrCodeRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.QrCodeViewModel
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.QrCodeListRoute

@Composable
fun MySuperApp(
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val state by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = (context.getFragmentActivity())

    MySuperAppTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Log.d("MainActivity", "onCreate: $innerPadding")
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Screen.SplashScreen.route
            ) {
                composable(route = Screen.SplashScreen.route) {
                    SplashRoute(
                        status = state,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.SplashScreen.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(route = Screen.LoginScreen.route) {
                    LoginRoute(
                        onNavigateHome = {
                            navController.navigate(Screen.HomeScreen.route) {
                                popUpTo(Screen.SplashScreen.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(route = Screen.HomeScreen.route) {
                    HomeRoute(
                        navController = navController,
                        activity = activity,
                        onNavigate = { route ->
                            navController.navigate(route)
                        }
                    )
                }

                composable(route = Screen.FinancialScreen.route) {
                    FinancialRoute(
                        onNavigateUp = {
                            navController.navigateUp()
                        },
                        onNewTransaction = {
                            navController.navigate(Screen.NewTransactionScreen.route)
                        }
                    )
                }

                composable(route = Screen.NewTransactionScreen.route) {
                    NewTransactionRoute(
                        onNavigateUp = {
                            navController.navigateUp()
                        }
                    )
                }

                composable(route = Screen.QrCodeListScreen.route) {
                    QrCodeListRoute(
                        navController = navController,
                        onNavigateQrCode = { qrCode ->
                            navController.navigate(Screen.QrCodeScreen.createRoute(qrCode))
                        }
                    )
                }

                composable(
                    route = Screen.QrCodeScreen.route,
                    arguments = listOf(
                        navArgument(Screen.QrCodeScreen.QR_CODE_DATA) {
                            type = NavType.StringType
                        }
                    )
                ) { parentEntry ->
                    val qrCodeDataJson =
                        parentEntry.arguments!!.getString(Screen.QrCodeScreen.QR_CODE_DATA)!!
                    val qrCode = com.google.gson.Gson().fromJson(qrCodeDataJson, QrCode::class.java)
                    val viewModel: QrCodeViewModel = hiltViewModel(parentEntry)
                    QrCodeRoute(
                        viewModel = viewModel,
                        navController = navController,
                        qrCode = qrCode
                    )
                }

                accessLogMapRoute()
            }
        }
    }
}