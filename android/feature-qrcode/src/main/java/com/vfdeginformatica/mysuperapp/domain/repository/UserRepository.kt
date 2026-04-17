package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings
import com.vfdeginformatica.mysuperapp.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun signUp(email: String, password: String, name: String): Resource<String?>
    suspend fun login(email: String, password: String): Resource<String?>
    suspend fun logout(): Boolean
    suspend fun sessionStatusFlow(): Flow<UserSession>
    suspend fun updateProfile(name: String, email: String): Resource<Unit>
    suspend fun getNotificationSettings(): Resource<NotificationSettings>
    suspend fun updateNotificationSettings(settings: NotificationSettings): Resource<Unit>
}