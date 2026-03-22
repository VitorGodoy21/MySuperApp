package com.vfdeginformatica.mysuperapp.domain.use_case

import androidx.fragment.app.FragmentActivity
import com.vfdeginformatica.mysuperapp.domain.repository.BiometricRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthenticateWithBiometricUseCase @Inject constructor(
    private val biometricRepository: BiometricRepository
) {
    /**
     * Executa a autenticação biométrica nativa do dispositivo
     * @param activity A atividade na qual o prompt será exibido
     * @return Flow com o resultado (true = autenticado, false = falha/cancelado)
     */
    operator fun invoke(activity: FragmentActivity): Flow<Boolean> {
        return biometricRepository.authenticateWithBiometric(activity)
    }
}

