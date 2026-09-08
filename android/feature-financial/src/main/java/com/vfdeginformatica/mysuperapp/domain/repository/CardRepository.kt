package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto

interface CardRepository {
    suspend fun getCards(): List<CardDto>?
    suspend fun addCard(card: CardDto): Resource<String>
    suspend fun updateCard(
        cardId: String,
        bank: String,
        paymentMethods: List<String>,
        finalCode: String,
        closingDay: Int,
        invoiceDueDate: Int,
    ): Resource<Unit>
}