package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto

interface CardDao {
    suspend fun getCards(): List<CardDto>?
}