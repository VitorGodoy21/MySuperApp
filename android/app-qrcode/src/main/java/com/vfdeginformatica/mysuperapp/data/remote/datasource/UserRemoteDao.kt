package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.domain.model.UserSession

interface UserRemoteDao {
    suspend fun createUser(uid: String, email: String, name: String)

    suspend fun getUserSession(id: String): UserSession?
}