package com.vfdeginformatica.mysuperapp.domain.use_case.login

import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class IsLoggedUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<Boolean> = flow {
        repository.sessionStatusFlow().collect { session ->
            emit(session.isLoggedIn)
        }
    }

}