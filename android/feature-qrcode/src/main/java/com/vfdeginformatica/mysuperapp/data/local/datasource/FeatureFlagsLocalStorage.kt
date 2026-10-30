package com.vfdeginformatica.mysuperapp.data.local.datasource

import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import kotlinx.coroutines.flow.Flow

/** Cache local das flags por usuário; `null` quando ainda não há cache para o uid. */
interface FeatureFlagsLocalStorage {
    fun observe(uid: String): Flow<FeatureFlags?>
    suspend fun save(uid: String, flags: FeatureFlags)
    suspend fun clear()
}
