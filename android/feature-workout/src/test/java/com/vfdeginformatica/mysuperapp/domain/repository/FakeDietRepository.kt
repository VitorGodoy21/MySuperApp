package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietRecord
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDietRepository : DietRepository {
    val owner = MutableStateFlow<String?>("owner")
    val diets = mutableMapOf<String, Diet>()
    var readError: String? = null
    var writeError: String? = null
    var saveGate: CompletableDeferred<Unit>? = null
    var writes = 0
    var deletes = 0

    override fun observeOwner() = owner

    override suspend fun getDiet(): Resource<DietRecord> {
        readError?.let { return Resource.Error(it) }
        val id = owner.value ?: return Resource.Error("Não autenticado")
        return Resource.Success(DietRecord(id, diets[id]))
    }

    override suspend fun saveDiet(ownerId: String, diet: Diet): Resource<Unit> {
        saveGate?.await()
        writeError?.let { return Resource.Error(it) }
        if (owner.value != ownerId) return Resource.Error("A conta mudou")
        writes++
        diets[ownerId] = diet
        return Resource.Success(Unit)
    }

    override suspend fun deleteDiet(ownerId: String): Resource<Unit> {
        writeError?.let { return Resource.Error(it) }
        if (owner.value != ownerId) return Resource.Error("A conta mudou")
        deletes++
        diets.remove(ownerId)
        return Resource.Success(Unit)
    }
}
