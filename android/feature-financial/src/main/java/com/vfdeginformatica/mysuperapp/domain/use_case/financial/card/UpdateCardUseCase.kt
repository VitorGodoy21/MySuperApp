package com.vfdeginformatica.mysuperapp.domain.use_case.financial.card

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateCardUseCase @Inject constructor(
    private val cardRepository: CardRepository
) {
    operator fun invoke(
        cardId: String,
        bank: String,
        paymentMethods: List<String>,
        finalCode: String,
        closingDay: Int,
        invoiceDueDate: Int,
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        when (val result = cardRepository.updateCard(
            cardId = cardId,
            bank = bank,
            paymentMethods = paymentMethods,
            finalCode = finalCode,
            closingDay = closingDay,
            invoiceDueDate = invoiceDueDate,
        )) {
            is Resource.Success -> emit(Resource.Success(Unit))
            is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao atualizar cartão"))
            is Resource.Loading -> Unit
        }
    }
}
