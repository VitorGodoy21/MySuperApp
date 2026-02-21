package com.vfdeginformatica.mysuperapp.domain.use_case.card

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetCardsUseCase @Inject constructor(
    private val repository: CardRepository
) {
    operator fun invoke(): Flow<Resource<List<CardDto>>> = flow {
        emit(Resource.Loading())
        val userTypes = repository.getCards()
        if (userTypes != null) {
            emit(Resource.Success(userTypes))
        } else {
            emit(Resource.Error("An unexpected error occurred"))
        }
    }
}