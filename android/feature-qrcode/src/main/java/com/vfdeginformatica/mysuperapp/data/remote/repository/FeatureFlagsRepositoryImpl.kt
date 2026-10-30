package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.local.datasource.FeatureFlagsLocalStorage
import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.data.remote.datasource.UserRemoteDao
import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import com.vfdeginformatica.mysuperapp.domain.repository.FeatureFlagsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FeatureFlagsRepositoryImpl(
    private val remoteDao: UserRemoteDao,
    private val localStorage: FeatureFlagsLocalStorage,
    private val sessionStorage: UserSessionSecureStorage,
) : FeatureFlagsRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<FeatureFlags> =
        sessionStorage.sessionFlow
            .map { if (it.isLoggedIn) it.id else "" }
            .distinctUntilChanged()
            .flatMapLatest { uid ->
                if (uid.isBlank()) flowOf(FeatureFlags())
                else localStorage.observe(uid).map { it ?: FeatureFlags() }
            }

    override suspend fun refresh(): Resource<FeatureFlags> {
        val session = sessionStorage.getUserSession()
        if (!session.isLoggedIn || session.id.isBlank()) {
            return Resource.Error("Usuário não autenticado")
        }
        return try {
            val flags = remoteDao.getFeatureFlags(session.id)
            localStorage.save(session.id, flags)
            Resource.Success(flags)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Não foi possível carregar as configurações")
        }
    }
}
