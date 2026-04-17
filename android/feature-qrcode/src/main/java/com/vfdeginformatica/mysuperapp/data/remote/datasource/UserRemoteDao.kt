package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings
import com.vfdeginformatica.mysuperapp.domain.model.UserSession

interface UserRemoteDao {
    suspend fun createUser(uid: String, email: String, name: String)
    suspend fun getUserSession(id: String): UserSession?
    suspend fun updateUser(uid: String, name: String, email: String)
    suspend fun getNotificationSettings(uid: String): NotificationSettings
    suspend fun updateNotificationSettings(uid: String, settings: NotificationSettings)
}