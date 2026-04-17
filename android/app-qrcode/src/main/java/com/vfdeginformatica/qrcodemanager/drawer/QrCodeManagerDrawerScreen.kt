package com.vfdeginformatica.qrcodemanager.drawer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun QrCodeManagerDrawerScreen(
    userName: String?,
    userEmail: String?,
    isOpen: Boolean,
    onToggle: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(isOpen) {
        scope.launch {
            if (isOpen) drawerState.open() else drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader(name = userName, email = userEmail)

                HorizontalDivider(
                    Modifier.height(1.dp),
                    DividerDefaults.Thickness,
                    DividerDefaults.color
                )

                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text("Perfil") },
                    selected = false,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil"
                        )
                    },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onToggle()
                        onProfile()
                    }
                )

                NavigationDrawerItem(
                    label = { Text("Notificações") },
                    selected = false,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificações"
                        )
                    },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onToggle()
                        onNotifications()
                    }
                )

                Spacer(Modifier.height(8.dp))

                HorizontalDivider(
                    Modifier.height(1.dp),
                    DividerDefaults.Thickness,
                    DividerDefaults.color
                )

                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    label = { Text("Sair") },
                    selected = false,
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sair"
                        )
                    },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onToggle()
                        onLogout()
                    }
                )
            }
        },
        content = content
    )
}

@Composable
private fun DrawerHeader(name: String?, email: String?) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = name?.ifEmpty { "Usuário" } ?: "Usuário",
            style = MaterialTheme.typography.titleMedium
        )
        if (!email.isNullOrEmpty()) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

