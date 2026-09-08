package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto

interface CardRepository {
    suspend fun getCards(): List<CardDto>?
}