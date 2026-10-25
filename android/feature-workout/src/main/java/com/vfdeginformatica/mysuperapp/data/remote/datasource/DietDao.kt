package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.DietDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.DietRecordDto
import kotlinx.coroutines.flow.Flow

interface DietDao {
    fun observeOwner(): Flow<String?>
    suspend fun getDiet(): Resource<DietRecordDto>
    suspend fun saveDiet(ownerId: String, diet: DietDto): Resource<Unit>
    suspend fun deleteDiet(ownerId: String): Resource<Unit>
}
