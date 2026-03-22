package com.vfdeginformatica.mysuperapp.data.local

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vfdeginformatica.mysuperapp.domain.repository.BiometricRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class BiometricRepositoryImpl @Inject constructor(
    private val context: Context
) : BiometricRepository {

    override fun authenticateWithBiometric(activity: FragmentActivity): Flow<Boolean> =
        callbackFlow {
            try {
                val biometricManager = BiometricManager.from(context)

                // Verificar se o dispositivo suporta biometria
                when (biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                )) {
                    BiometricManager.BIOMETRIC_SUCCESS -> {
                        // Dispositivo suporta biometria, prosseguir com autenticação
                        setupBiometricPrompt(activity, this)
                    }

                    else -> {
                        // Dispositivo não suporta biometria
                        trySend(false)
                        close()
                    }
                }
            } catch (e: Exception) {
                Log.e("BiometricRepository", "Error during biometric authentication", e)
                // Tratamento de erro")
                trySend(false)
                close()
            }

            awaitClose()
        }

    override suspend fun isBiometricAvailable(): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    private fun setupBiometricPrompt(
        activity: FragmentActivity,
        callback: kotlinx.coroutines.channels.SendChannel<Boolean>
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    callback.trySend(true)
                    callback.close()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    callback.trySend(false)
                    callback.close()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    callback.trySend(false)
                    callback.close()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticação Biométrica")
            .setSubtitle("Escaneie seu dedo para acessar")
            .setDescription("Use a biometria do dispositivo para continuar")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

