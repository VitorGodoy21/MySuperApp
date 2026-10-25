package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toDto
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.data.remote.datasource.DietDao
import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietRecord
import com.vfdeginformatica.mysuperapp.domain.repository.DietRepository

class DietRepositoryImpl(private val dao: DietDao) : DietRepository {
    override fun observeOwner() = dao.observeOwner()

    override suspend fun getDiet(): Resource<DietRecord> = when (val result = dao.getDiet()) {
        is Resource.Success -> {
            val record = result.data
            if (record == null) Resource.Error("Resposta inválida ao carregar dieta")
            else Resource.Success(DietRecord(record.ownerId, record.diet?.toModel()))
        }
        is Resource.Error -> Resource.Error(result.message ?: "Erro ao carregar dieta")
        is Resource.Loading -> Resource.Error("Resposta incompleta ao carregar dieta")
    }

    override suspend fun saveDiet(ownerId: String, diet: Diet) = dao.saveDiet(ownerId, diet.toDto())
    override suspend fun deleteDiet(ownerId: String) = dao.deleteDiet(ownerId)
}
