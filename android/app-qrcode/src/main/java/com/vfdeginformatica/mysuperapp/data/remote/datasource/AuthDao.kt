package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.common.Resource

interface AuthDao {
    suspend fun createUser(email: String, password: String): Resource<String?>

    suspend fun login(email: String, password: String): String?

    suspend fun logout(): Boolean
}