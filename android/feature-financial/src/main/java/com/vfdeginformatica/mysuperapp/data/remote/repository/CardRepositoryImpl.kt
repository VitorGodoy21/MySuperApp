package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDao
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository

class CardRepositoryImpl(
    private val cardDao: CardDao
) : CardRepository {
    override suspend fun getCards(): List<CardDto>? {
        return cardDao.getCards()
    }

    override suspend fun addCard(card: CardDto): Resource<String> {
        return cardDao.addCard(card)
    }

    override suspend fun updateCard(
        cardId: String,
        bank: String,
        paymentMethods: List<String>,
        finalCode: String,
        closingDay: Int,
        invoiceDueDate: Int,
    ): Resource<Unit> {
        return cardDao.updateCard(cardId, bank, paymentMethods, finalCode, closingDay, invoiceDueDate)
    }
}