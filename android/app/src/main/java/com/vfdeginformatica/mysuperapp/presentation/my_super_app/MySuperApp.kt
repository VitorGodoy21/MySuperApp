package com.vfdeginformatica.mysuperapp.presentation.my_super_app

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.vfdeginformatica.mysuperapp.presentation.screen.card_detail.CardDetailRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.CardInvoiceRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.card_list.CardListRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.category_detail.CategoryDetailRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.BodyMeasurementDetailRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.BodyMeasurementListRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.ExerciseDetailRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.ExerciseListRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.WorkoutDetailRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.WorkoutHubRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_view.DietViewRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.DietDetailRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.WorkoutViewRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.WorkoutListRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.category_list.CategoryListRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.FinancialRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.home.HomeRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.login.LoginRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.muralCommentsRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.new_card.NewCardRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.NewTransactionRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.notifications.NotificationsRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.QrCodeRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.QrCodeViewModel
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.QrCodeListRoute
import com.vfdeginformatica.mysuperapp.presentation.screen.settings.SettingsRoute
import com.vfdeginformatica.mysuperapp.nfc.navigation.SharedNfcRoutes
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.NfcRoute

@Composable
fun MySuperApp(
    sessionViewModel: SessionViewModel = hiltViewModel(),
    deepLinkViewModel: NotificationDeepLinkViewModel = hiltViewModel()
) {
    val state by sessionViewModel.uiState.collectAsStateWithLifecycle()
    val pendingQrCodeId by deepLinkViewModel.pendingQrCodeId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = (context.getFragmentActivity())

    MySuperAppTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Log.d("MainActivity", "onCreate: $innerPadding")
            val navController = rememberNavController()

            // Toque em notificação de QR Code: resolve após a sessão estar logada.
            LaunchedEffect(pendingQrCodeId, state.isLoggedIn) {
                if (pendingQrCodeId != null && state.isLoggedIn) {
                    deepLinkViewModel.consumePending(activity)
                }
            }

            LaunchedEffect(Unit) {
                deepLinkViewModel.effect.collect { effect ->
                    when (effect) {
                        is NotificationDeepLinkEffect.OpenQrCode ->
                            navController.navigate(Screen.QrCodeScreen.createRoute(effect.qrCode)) {
                                launchSingleTop = true
                            }

                        is NotificationDeepLinkEffect.Navigate ->
                            navController.navigate(effect.route) { launchSingleTop = true }

                        is NotificationDeepLinkEffect.ShowToast ->
                            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

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
                        onNewTransaction = { type ->
                            navController.navigate(Screen.NewTransactionScreen.createRoute(type.name))
                        },
                        onCardInvoice = { cardId, invoiceMonth ->
                            navController.navigate(
                                Screen.CardInvoiceScreen.createRoute(cardId, invoiceMonth)
                            )
                        }
                    )
                }

                composable(
                    route = Screen.CardInvoiceScreen.route,
                    arguments = listOf(
                        navArgument(Screen.CardInvoiceScreen.CARD_ID) {
                            type = NavType.StringType
                        },
                        navArgument(Screen.CardInvoiceScreen.INVOICE_MONTH) {
                            type = NavType.StringType
                        }
                    )
                ) {
                    CardInvoiceRoute(
                        onNavigateUp = {
                            navController.navigateUp()
                        }
                    )
                }

                composable(
                    route = Screen.NewTransactionScreen.route,
                    arguments = listOf(
                        navArgument(Screen.NewTransactionScreen.TRANSACTION_TYPE) {
                            type = NavType.StringType
                        }
                    )
                ) {
                    NewTransactionRoute(
                        onNavigateUp = {
                            navController.navigateUp()
                        },
                        onNewCard = {
                            navController.navigate(Screen.NewCardScreen.route)
                        }
                    )
                }

                composable(route = Screen.NewCardScreen.route) {
                    NewCardRoute(
                        onNavigateUp = {
                            navController.navigateUp()
                        }
                    )
                }

                composable(route = Screen.CardListScreen.route) {
                    CardListRoute(
                        onNavigateUp = {
                            navController.navigateUp()
                        },
                        onNavigateToCardDetail = { cardId ->
                            navController.navigate(Screen.CardDetailScreen.createRoute(cardId))
                        },
                        onNavigateToNewCard = {
                            navController.navigate(Screen.NewCardScreen.route)
                        }
                    )
                }

                composable(
                    route = Screen.CardDetailScreen.route,
                    arguments = listOf(
                        navArgument(Screen.CardDetailScreen.CARD_ID) {
                            type = NavType.StringType
                        }
                    )
                ) {
                    CardDetailRoute(
                        onNavigateUp = {
                            navController.navigateUp()
                        }
                    )
                }

                composable(route = Screen.CategoryListScreen.route) {
                    CategoryListRoute(
                        onNavigateUp = {
                            navController.navigateUp()
                        },
                        onNavigateToCategoryDetail = { categoryId ->
                            navController.navigate(
                                Screen.CategoryDetailScreen.createRoute(categoryId)
                            )
                        },
                        onNavigateToNewCategory = {
                            navController.navigate(Screen.CategoryDetailScreen.createRoute())
                        }
                    )
                }

                composable(
                    route = Screen.CategoryDetailScreen.route,
                    arguments = listOf(
                        navArgument(Screen.CategoryDetailScreen.CATEGORY_ID) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    CategoryDetailRoute(
                        onNavigateUp = {
                            navController.navigateUp()
                        }
                    )
                }

                // Academia (feature-workout)
                composable(route = Screen.WorkoutHubScreen.route) {
                    WorkoutHubRoute(
                        onNavigateUp = { navController.navigateUp() },
                        onNavigateToWorkout = { workoutId ->
                            navController.navigate(Screen.WorkoutViewScreen.createRoute(workoutId))
                        },
                        onNavigateToDiet = {
                            navController.navigate(Screen.DietViewScreen.route)
                        }
                    )
                }

                composable(route = Screen.DietViewScreen.route) {
                    DietViewRoute(onNavigateUp = { navController.navigateUp() })
                }

                composable(route = Screen.DietDetailScreen.route) {
                    DietDetailRoute(onNavigateUp = { navController.navigateUp() })
                }

                composable(
                    route = Screen.WorkoutViewScreen.route,
                    arguments = listOf(
                        navArgument(Screen.WorkoutViewScreen.WORKOUT_ID) { type = NavType.StringType }
                    )
                ) {
                    WorkoutViewRoute(
                        onNavigateUp = { navController.navigateUp() }
                    )
                }

                composable(route = Screen.BodyMeasurementListScreen.route) {
                    BodyMeasurementListRoute(
                        onNavigateUp = { navController.navigateUp() },
                        onNavigateToDetail = { measurementId ->
                            navController.navigate(
                                Screen.BodyMeasurementDetailScreen.createRoute(measurementId)
                            )
                        }
                    )
                }

                composable(
                    route = Screen.BodyMeasurementDetailScreen.route,
                    arguments = listOf(
                        navArgument(Screen.BodyMeasurementDetailScreen.MEASUREMENT_ID) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    BodyMeasurementDetailRoute(
                        onNavigateUp = { navController.navigateUp() }
                    )
                }

                composable(route = Screen.ExerciseListScreen.route) {
                    ExerciseListRoute(
                        onNavigateUp = { navController.navigateUp() },
                        onNavigateToDetail = { exerciseId ->
                            navController.navigate(Screen.ExerciseDetailScreen.createRoute(exerciseId))
                        }
                    )
                }

                composable(
                    route = Screen.ExerciseDetailScreen.route,
                    arguments = listOf(
                        navArgument(Screen.ExerciseDetailScreen.EXERCISE_ID) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    ExerciseDetailRoute(
                        onNavigateUp = { navController.navigateUp() }
                    )
                }

                composable(route = Screen.WorkoutListScreen.route) {
                    WorkoutListRoute(
                        onNavigateUp = { navController.navigateUp() },
                        onNavigateToDetail = { workoutId ->
                            navController.navigate(Screen.WorkoutDetailScreen.createRoute(workoutId))
                        }
                    )
                }

                composable(
                    route = Screen.WorkoutDetailScreen.route,
                    arguments = listOf(
                        navArgument(Screen.WorkoutDetailScreen.WORKOUT_ID) {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) {
                    WorkoutDetailRoute(
                        onNavigateUp = { navController.navigateUp() }
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

                accessLogMapRoute(navController)
                muralCommentsRoute(navController)

                composable(route = SharedNfcRoutes.NFC_SCREEN) {
                    NfcRoute(onNavigateUp = { navController.navigateUp() })
                }

                composable(route = Screen.SettingsScreen.route) {
                    SettingsRoute(
                        onNavigateUp = { navController.navigateUp() },
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }

                composable(route = Screen.NotificationsScreen.route) {
                    NotificationsRoute(onNavigateUp = { navController.navigateUp() })
                }
            }
        }
    }
}