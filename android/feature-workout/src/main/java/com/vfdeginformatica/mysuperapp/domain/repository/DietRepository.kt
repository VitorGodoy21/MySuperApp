package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietRecord
import kotlinx.coroutines.flow.Flow

interface DietRepository {
    fun observeOwner(): Flow<String?>
    suspend fun getDiet(): Resource<DietRecord>
    suspend fun saveDiet(ownerId: String, diet: Diet): Resource<Unit>
    suspend fun deleteDiet(ownerId: String): Resource<Unit>
}
