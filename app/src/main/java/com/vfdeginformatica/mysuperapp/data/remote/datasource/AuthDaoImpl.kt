package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.google.firebase.auth.FirebaseAuth
import com.vfdeginformatica.mysuperapp.common.Resource
import kotlinx.coroutines.tasks.await

class AuthDaoImpl(
    private val auth: FirebaseAuth
) : AuthDao {
    override suspend fun createUser(
        email: String,
        password: String
    ): Resource<String?> {
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            if (result.user?.uid != null) {
                return Resource.Success(result.user?.uid)
            }

            return Resource.Error("An unexpected error occurred")
        } catch (e: Exception) {
            return Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    override suspend fun login(email: String, password: String): String? {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            if (result.user?.uid != null) {
                return result.user?.uid
            }

            return null
        } catch (e: Exception) {
            return null
        }
    }

    override suspend fun logout(): Boolean {
        auth.signOut()
        return true
    }
}