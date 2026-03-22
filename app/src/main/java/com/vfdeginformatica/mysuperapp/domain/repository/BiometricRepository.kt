package com.vfdeginformatica.mysuperapp.domain.repository

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow

interface BiometricRepository {
    /**
     * Realiza autenticação biométrica nativa do dispositivo
     * @param activity A atividade na qual o prompt será exibido
     * @return Flow com o resultado da autenticação (true = sucesso, false = falha/cancelado)
     */
    fun authenticateWithBiometric(activity: FragmentActivity): Flow<Boolean>

    /**
     * Verifica se o dispositivo suporta autenticação biométrica
     */
    suspend fun isBiometricAvailable(): Boolean
}

