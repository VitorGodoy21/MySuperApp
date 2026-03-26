package com.vfdeginformatica.mysuperapp.domain.use_case.login

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(
        email: String,
        password: String
    ): Flow<Resource<String?>> = flow {
        emit(Resource.Loading())
        when (val result = repository.login(email, password)) {
            is Resource.Success<String?> -> {
                emit(Resource.Success(result.data ?: String()))
            }

            is Resource.Error -> {
                emit(Resource.Error(result.message ?: String()))
            }

            is Resource.Loading -> {}
        }
    }
}