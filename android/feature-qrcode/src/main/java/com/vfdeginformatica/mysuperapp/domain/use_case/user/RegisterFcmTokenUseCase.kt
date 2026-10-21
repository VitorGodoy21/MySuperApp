package com.vfdeginformatica.mysuperapp.domain.use_case.user

import com.google.firebase.messaging.FirebaseMessaging
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Registra o token FCM do dispositivo no documento do usuário logado
 * (`users/{uid}.fcmTokens.{token}` + `fcmToken` legado).
 * Quando [token] é nulo, obtém o token atual do [FirebaseMessaging].
 */
class RegisterFcmTokenUseCase @Inject constructor(
    private val repository: UserRepository
) {
    suspend operator fun invoke(token: String? = null): Resource<Unit> {
        val resolved = token ?: try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            return Resource.Error(e.message ?: "Não foi possível obter o token de notificação")
        }
        if (resolved.isNullOrBlank()) return Resource.Error("Token de notificação vazio")
        return repository.registerFcmToken(resolved)
    }
}
