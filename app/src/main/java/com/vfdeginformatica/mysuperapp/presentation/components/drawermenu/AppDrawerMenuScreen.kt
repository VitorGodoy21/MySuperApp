package com.vfdeginformatica.mysuperapp.presentation.components.drawermenu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.rememberCoroutineScope
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

                NavigationDrawerItem(
                    label = { Text("Controle Financeiro") },
                    selected = false,
                    onClick = { onFinancialManagement.invoke() },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "FinancialManagement"
                        )
                    }
                )

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