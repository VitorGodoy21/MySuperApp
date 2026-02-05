package com.vfdeginformatica.mysuperapp.presentation.my_super_app

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.presentation.common.ui.theme.MySuperAppTheme
import com.vfdeginformatica.mysuperapp.presentation.screen.home.HomeRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.login.LoginRoute

@Composable
fun MySuperApp(
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val state by sessionViewModel.uiState.collectAsStateWithLifecycle()

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
                        onNavigate = { route ->
                            //navController.navigate(route)
                        }
                    )
                }
            }
        }
    }
}