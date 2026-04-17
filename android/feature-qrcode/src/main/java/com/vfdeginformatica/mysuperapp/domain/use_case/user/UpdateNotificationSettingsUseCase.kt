package com.vfdeginformatica.mysuperapp.domain.use_case.user

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateNotificationSettingsUseCase @Inject constructor(
    private val repository: UserRepository
) {
    operator fun invoke(settings: NotificationSettings): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        emit(repository.updateNotificationSettings(settings))
    }
}

