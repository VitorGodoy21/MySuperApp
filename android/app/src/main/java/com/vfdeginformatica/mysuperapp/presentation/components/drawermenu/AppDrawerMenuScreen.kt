package com.vfdeginformatica.mysuperapp.presentation.components.drawermenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Contactless
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract.AppDrawerMenuEvent
import com.vfdeginformatica.mysuperapp.presentation.components.drawermenu.contract.AppDrawerMenuUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerMenuScreen(
    state: AppDrawerMenuUiState,
    onEvent: (AppDrawerMenuEvent) -> Unit,
    onLogout: () -> Unit,
    onHome: () -> Unit,
    onFinancialManagement: () -> Unit,
    onFinancialCards: () -> Unit,
    onFinancialCategories: () -> Unit,
    onWorkoutBodyMeasurements: () -> Unit,
    onWorkoutExercises: () -> Unit,
    onWorkoutWorkouts: () -> Unit,
    onQrCode: () -> Unit,
    onNfc: () -> Unit,
    onSettings: () -> Unit,
    onResume: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Open
    )

    val scope = rememberCoroutineScope()

    LaunchedEffect(state.isOpen) {
        scope.launch {
            if (state.isOpen && drawerState.isOpen) {
                drawerState.close()
            } else {
                drawerState.open()
            }
        }
    }

    LaunchedEffect(state.closeDrawer) {
        scope.launch {
            if (state.closeDrawer) {
                drawerState.close()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> onResume.invoke()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader(
                    name = state.userName,
                    email = state.userEmail,
                    avatarUrl = state.avatarUrl
                )

                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = false,
                    onClick = { onHome.invoke() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "home"
                        )
                    }
                )

                var isFinancialExpanded by rememberSaveable { mutableStateOf(false) }

                NavigationDrawerItem(
                    label = { Text("Controle Financeiro") },
                    selected = false,
                    onClick = { isFinancialExpanded = !isFinancialExpanded },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "FinancialManagement"
                        )
                    },
                    badge = {
                        Icon(
                            imageVector = if (isFinancialExpanded) {
                                Icons.Default.ExpandLess
                            } else {
                                Icons.Default.ExpandMore
                            },
                            contentDescription = if (isFinancialExpanded) {
                                "Recolher finanças"
                            } else {
                                "Expandir finanças"
                            }
                        )
                    }
                )

                if (isFinancialExpanded) {
                    NavigationDrawerItem(
                        label = { Text("Dashboard") },
                        selected = false,
                        onClick = { onFinancialManagement.invoke() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "FinancialDashboard"
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Cartões") },
                        selected = false,
                        onClick = { onFinancialCards.invoke() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "FinancialCards"
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Categorias") },
                        selected = false,
                        onClick = { onFinancialCategories.invoke() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "FinancialCategories"
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }

                var isWorkoutExpanded by rememberSaveable { mutableStateOf(false) }

                NavigationDrawerItem(
                    label = { Text("Academia") },
                    selected = false,
                    onClick = { isWorkoutExpanded = !isWorkoutExpanded },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Workout"
                        )
                    },
                    badge = {
                        Icon(
                            imageVector = if (isWorkoutExpanded) {
                                Icons.Default.ExpandLess
                            } else {
                                Icons.Default.ExpandMore
                            },
                            contentDescription = if (isWorkoutExpanded) {
                                "Recolher academia"
                            } else {
                                "Expandir academia"
                            }
                        )
                    }
                )

                if (isWorkoutExpanded) {
                    NavigationDrawerItem(
                        label = { Text("Dados físicos") },
                        selected = false,
                        onClick = { onWorkoutBodyMeasurements.invoke() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = "WorkoutBodyMeasurements"
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Exercícios") },
                        selected = false,
                        onClick = { onWorkoutExercises.invoke() },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "WorkoutExercises"
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("Treinos") },
                        selected = false,
                        onClick = { onWorkoutWorkouts.invoke() },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.EventNote,
                                contentDescription = "WorkoutWorkouts"
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }

                var isSmartTagsExpanded by rememberSaveable { mutableStateOf(false) }

                NavigationDrawerItem(
                    label = { Text("Smart Tags") },
                    selected = false,
                    onClick = { isSmartTagsExpanded = !isSmartTagsExpanded },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Contactless,
                            contentDescription = "SmartTags"
                        )
                    },
                    badge = {
                        Icon(
                            imageVector = if (isSmartTagsExpanded) {
                                Icons.Default.ExpandLess
                            } else {
                                Icons.Default.ExpandMore
                            },
                            contentDescription = if (isSmartTagsExpanded) {
                                "Recolher smart tags"
                            } else {
                                "Expandir smart tags"
                            }
                        )
                    }
                )

                if (isSmartTagsExpanded) {
                    NavigationDrawerItem(
                        label = { Text("QR Code") },
                        selected = false,
                        onClick = { onQrCode.invoke() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QrCode"
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp)
                    )

                    NavigationDrawerItem(
                        label = { Text("NFC") },
                        selected = false,
                        onClick = { onNfc.invoke() },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Nfc,
                                contentDescription = "Nfc"
                            )
                        },
                        modifier = Modifier.padding(start = 24.dp)
                    )
                }

                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = { onSettings.invoke() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "settings"
                        )
                    }
                )

                HorizontalDivider(
                    Modifier.height(8.dp),
                    DividerDefaults.Thickness,
                    DividerDefaults.color
                )


                state.items.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = false,//item.route == state.selectedRoute,
                        icon = {
                            item.icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = item.label
                                )
                            }
                        },
                        onClick = {
                            onEvent(AppDrawerMenuEvent.ClickItem(item))
                        }
                    )
                }

                if (state.items.isNotEmpty()) {
                    HorizontalDivider(
                        Modifier.height(8.dp),
                        DividerDefaults.Thickness,
                        DividerDefaults.color
                    )
                }

                NavigationDrawerItem(
                    label = { Text("Sair") },
                    selected = false,
                    onClick = { onLogout.invoke() },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "logout"
                        )

                    },
                )
            }
        },
        content = content
    )
}

@Composable
private fun DrawerHeader(name: String?, email: String?, avatarUrl: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(name ?: "Usuário", style = MaterialTheme.typography.titleMedium)
        Text(email ?: "", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(Modifier.height(8.dp), DividerDefaults.Thickness, DividerDefaults.color)
    }
}