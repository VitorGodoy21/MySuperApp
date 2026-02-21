package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDao
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository

class CardRepositoryImpl(
    private val cardDao: CardDao
) : CardRepository {
    override suspend fun getCards(): List<CardDto>? {
        return cardDao.getCards()
    }
}