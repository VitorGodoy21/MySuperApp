package com.vfdeginformatica.mysuperapp.data.local.datasource

import com.vfdeginformatica.mysuperapp.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface UserSessionSecureStorage {
    val sessionFlow: Flow<UserSession>

    suspend fun getUserSession(): UserSession

    suspend fun write(s: UserSession)

    suspend fun clear()
}