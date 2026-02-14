package com.vfdeginformatica.mysuperapp.domain.use_case.financial

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import javax.inject.Inject

class NewTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(
        title: String? = null,
        description: String? = null,
        transactionType: String? = null,
        paymentMethod: String,
        amount: BigDecimal,
        isRecurring: Boolean,
        transactionDate: Timestamp? = null,
        category: List<String>? = null,
        status: String? = null,
        cardId: String? = null,
        invoiceMonth: String? = null,
        installmentGroupId: String? = null,
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val transactionDto = TransactionDto(
            title = title,
            description = description,
            transactionType = transactionType,
            paymentMethod = paymentMethod,
            amount = amount,
            isRecurring = isRecurring,
            transactionDate = transactionDate,
            category = category,
            status = status,
            cardId = cardId,
            invoiceMonth = invoiceMonth,
            installmentGroupId = installmentGroupId,
        )
        when (val result = repository.newTransaction(transactionDto)) {
            is Resource.Success -> {
                emit(Resource.Success(Unit))
            }

            is Resource.Error -> {
                emit(Resource.Error(result.message ?: "An unexpected error occurred"))
            }

            is Resource.Loading -> {
                emit(Resource.Loading())
            }
        }
    }

}