package com.vfdeginformatica.mysuperapp.domain.use_case.login

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.user.RegisterFcmTokenUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: UserRepository,
    private val registerFcmTokenUseCase: RegisterFcmTokenUseCase
) {
    operator fun invoke(
        email: String,
        password: String
    ): Flow<Resource<String?>> = flow {
        emit(Resource.Loading())
        when (val result = repository.login(email, password)) {
            is Resource.Success<String?> -> {
                // O logout remove o token FCM; o login feito com o app já aberto não passa
                // por onResume, então re-registra aqui (best-effort, não bloqueia o login).
                runCatching { registerFcmTokenUseCase() }
                emit(Resource.Success(result.data ?: String()))
            }

            is Resource.Error -> {
                emit(Resource.Error(result.message ?: String()))
            }

            is Resource.Loading -> {}
        }
    }
}