package com.vfdeginformatica.mysuperapp.domain.use_case.user

import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetUserSessionUseCase @Inject constructor(
    private val localUserRepository: UserSessionSecureStorage
) {
    operator fun invoke(): Flow<UserSession> = flow {
        val userSession = localUserRepository.sessionFlow.first()
        emit(userSession)
    }
}