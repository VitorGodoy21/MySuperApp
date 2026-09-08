package com.vfdeginformatica.mysuperapp.domain.use_case.financial.card

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.NewTransactionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Cadastra um cartão e, opcionalmente, as transações iniciais:
 * - saldo inicial → receita (aparece no dashboard do mês)
 * - fatura atual (crédito) → despesa de crédito com invoiceMonth calculado pelo closingDay
 * Ambas ficam editáveis pelo dialog de edição de transações.
 */
class AddCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(
        bank: String,
        paymentMethods: List<String>,
        finalCode: String,
        closingDay: Int,
        invoiceDueDate: Int,
        initialBalance: Double,
        currentInvoice: Double,
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())

        val cardResult = cardRepository.addCard(
            CardDto(
                bank = bank,
                finalCode = finalCode,
                paymentMethods = paymentMethods,
                invoiceDueDate = invoiceDueDate,
                allowsDeposit = true,
                closingDay = closingDay
            )
        )
        val cardId = when (cardResult) {
            is Resource.Success -> cardResult.data.orEmpty()
            is Resource.Error -> {
                emit(Resource.Error(cardResult.message ?: "Erro ao cadastrar cartão"))
                return@flow
            }

            is Resource.Loading -> return@flow
        }

        val now = Timestamp.now()
        val hasCredit = paymentMethods.contains("CREDIT")
        val initialTransactions = buildList {
            if (initialBalance > 0.0) {
                add(
                    TransactionDto(
                        title = "Saldo inicial — $bank",
                        description = "Saldo do cartão no momento do cadastro",
                        transactionType = "INCOME",
                        paymentMethod = "",
                        amount = initialBalance,
                        isRecurring = false,
                        transactionDate = now,
                        category = emptyList(),
                        status = "pending",
                        cardId = cardId
                    )
                )
            }
            if (hasCredit && currentInvoice > 0.0) {
                val invoiceMonth = NewTransactionUseCase.computeInvoiceMonth(now, closingDay)
                    .format(NewTransactionUseCase.INVOICE_MONTH_FORMAT)
                add(
                    TransactionDto(
                        title = "Fatura atual — $bank",
                        description = "Fatura em aberto no momento do cadastro",
                        transactionType = "EXPENSE",
                        paymentMethod = "CREDIT",
                        amount = currentInvoice,
                        isRecurring = false,
                        transactionDate = now,
                        category = emptyList(),
                        status = "pending",
                        cardId = cardId,
                        invoiceMonth = invoiceMonth
                    )
                )
            }
        }

        if (initialTransactions.isNotEmpty()) {
            when (val txResult = transactionRepository.newTransactions(initialTransactions)) {
                is Resource.Error -> {
                    emit(
                        Resource.Error(
                            "Cartão criado, mas houve erro nas transações iniciais: " +
                                    (txResult.message ?: "erro inesperado")
                        )
                    )
                    return@flow
                }

                else -> Unit
            }
        }

        emit(Resource.Success(Unit))
    }
}
