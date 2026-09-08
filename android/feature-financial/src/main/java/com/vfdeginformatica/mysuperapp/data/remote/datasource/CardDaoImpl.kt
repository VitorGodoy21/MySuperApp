package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import kotlinx.coroutines.tasks.await

class CardDaoImpl(
    private val db: FirebaseFirestore
) : CardDao {
    override suspend fun getCards(): List<CardDto> {
        val snap = db.collection("cards").get().await()
        return snap.documents.mapNotNull { doc ->
            doc.toObject(CardDto::class.java)
        }
    }
}