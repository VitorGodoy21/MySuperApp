package com.vfdeginformatica.mysuperapp.domain.use_case.user

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(name: String, email: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        emit(repository.updateProfile(name.trim(), email.trim()))
    }
}

