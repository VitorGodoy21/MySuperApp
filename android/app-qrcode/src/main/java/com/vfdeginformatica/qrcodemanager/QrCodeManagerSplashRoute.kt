package com.vfdeginformatica.qrcodemanager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun QrCodeManagerSplashRoute(
    status: QrCodeManagerSessionUiState,
    onNavigate: (String) -> Unit
) {
    if (status.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    if (status.isLoggedIn) {
        onNavigate(QrCodeManagerScreen.QrCodeList.route)
    }

    if (status.isLoggedOut) {
        onNavigate(QrCodeManagerScreen.Login.route)
    }
}

