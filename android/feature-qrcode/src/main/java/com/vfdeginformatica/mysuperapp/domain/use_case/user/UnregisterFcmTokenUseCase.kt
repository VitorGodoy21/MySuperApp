package com.vfdeginformatica.mysuperapp.domain.use_case.user

import com.google.firebase.messaging.FirebaseMessaging
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Remove o token FCM deste dispositivo do usuário logado (usado no logout,
 * para que o dispositivo deslogado pare de receber push).
 */
class UnregisterFcmTokenUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(): Resource<Unit> {
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            return Resource.Error(e.message ?: "Não foi possível obter o token de notificação")
        }
        if (token.isNullOrBlank()) return Resource.Error("Token de notificação vazio")
        return repository.unregisterFcmToken(token)
    }
}
