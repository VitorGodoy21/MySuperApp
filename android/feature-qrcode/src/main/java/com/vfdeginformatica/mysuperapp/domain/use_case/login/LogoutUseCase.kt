package com.vfdeginformatica.mysuperapp.domain.use_case.login

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.user.UnregisterFcmTokenUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val repository: UserRepository,
    private val unregisterFcmTokenUseCase: UnregisterFcmTokenUseCase
) {
    operator fun invoke(): Flow<Resource<String?>> = flow {
        emit(Resource.Loading())
        // Best-effort: remove o token FCM antes do sign-out; falha não bloqueia o logout.
        runCatching { unregisterFcmTokenUseCase() }
        val result = repository.logout()
        when (result) {
            true -> {
                emit(Resource.Success("Logout realizado com sucesso"))
            }

            false -> {
                emit(Resource.Error("Não foi possível realizar o logout"))
            }
        }
    }
}