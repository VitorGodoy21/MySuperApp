package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto

interface CardDao {
    suspend fun getCards(): List<CardDto>?

    /** Cria o cartão e retorna o id do documento criado. */
    suspend fun addCard(card: CardDto): Resource<String>

    /** Atualiza os campos de configuração do cartão. */
    suspend fun updateCard(
        cardId: String,
        bank: String,
        paymentMethods: List<String>,
        finalCode: String,
        closingDay: Int,
        invoiceDueDate: Int,
    ): Resource<Unit>
}