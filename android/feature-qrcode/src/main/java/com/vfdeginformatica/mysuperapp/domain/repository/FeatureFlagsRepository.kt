package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import kotlinx.coroutines.flow.Flow

interface FeatureFlagsRepository {
    /** Flags do usuário logado a partir do cache local; tudo habilitado sem cache ou sem sessão. */
    fun observe(): Flow<FeatureFlags>

    /** Busca as flags remotas e atualiza o cache. Em falha o cache anterior é mantido. */
    suspend fun refresh(): Resource<FeatureFlags>
}
