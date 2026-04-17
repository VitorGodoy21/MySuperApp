package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.data.remote.datasource.AuthDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.UserRemoteDao
import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings
import com.vfdeginformatica.mysuperapp.domain.model.UserSession
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class UserRepositoryImpl(
    val userDao: UserRemoteDao,
    val authDao: AuthDao,
    val userSessionSecureStorage: UserSessionSecureStorage,
    val userRemoteDao: UserRemoteDao
) : UserRepository {
    override suspend fun signUp(
        email: String,
        password: String,
        name: String
    ): Resource<String?> {
        val result = authDao.createUser(email, password)
        try {
            when (result) {
                is Resource.Success -> {
                    userRemoteDao.createUser(result.data ?: String(), email, name)
                    return Resource.Success("Success")
                }

                is Resource.Error -> {
                    return Resource.Error(result.message ?: "An unexpected error occurred")
                }

                is Resource.Loading -> {}
            }

        } catch (e: Exception) {
            return Resource.Error(e.message.toString())
        }

        return Resource.Error("An unexpected error occurred")
    }

    override suspend fun login(
        email: String,
        password: String
    ): Resource<String?> {
        val id = authDao.login(email, password)
        try {
            val userSession = userDao.getUserSession(id ?: String())
            if (userSession != null) {
                userSessionSecureStorage.write(userSession)
                return Resource.Success("success!")
            } else {
                return Resource.Error("Error to get user session")
            }
        } catch (e: Exception) {
            return Resource.Error(e.message.toString())
        }
    }

    override suspend fun logout(): Boolean {
        userSessionSecureStorage.clear()
        return authDao.logout()
    }

    override suspend fun sessionStatusFlow(): Flow<UserSession> {
        return userSessionSecureStorage.sessionFlow
    }

    override suspend fun updateProfile(name: String, email: String): Resource<Unit> {
        return try {
            val uid = userSessionSecureStorage.getUserSession().id
            if (uid.isEmpty()) return Resource.Error("Usuário não autenticado")
            userRemoteDao.updateUser(uid, name, email)
            val current = userSessionSecureStorage.getUserSession()
            userSessionSecureStorage.write(current.copy(name = name, email = email))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Erro ao atualizar perfil")
        }
    }

    override suspend fun getNotificationSettings(): Resource<NotificationSettings> {
        return try {
            val uid = userSessionSecureStorage.getUserSession().id
            if (uid.isEmpty()) return Resource.Error("Usuário não autenticado")
            Resource.Success(userRemoteDao.getNotificationSettings(uid))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Erro ao carregar preferências de notificações")
        }
    }

    override suspend fun updateNotificationSettings(settings: NotificationSettings): Resource<Unit> {
        return try {
            val uid = userSessionSecureStorage.getUserSession().id
            if (uid.isEmpty()) return Resource.Error("Usuário não autenticado")
            userRemoteDao.updateNotificationSettings(uid, settings)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Erro ao atualizar preferências de notificações")
        }
    }
}