package com.vfdeginformatica.mysuperapp.presentation.my_super_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SplashRoute(
    status: SessionUiState,
    onNavigate: (String) -> Unit
) {

    if (status.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    if (status.isLoggedIn) {
        //onNavigate(Screen.HomeScreen.route)
        Text("LOGADO")
    }

    if (status.isLoggedOut) {
        //onNavigate(Screen.LoginScreen.route)
        Text("PRECISA LOGAR")
    }
}