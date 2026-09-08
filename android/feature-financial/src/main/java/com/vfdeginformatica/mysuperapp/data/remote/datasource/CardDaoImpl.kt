package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.common.Resource
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

    override suspend fun addCard(card: CardDto): Resource<String> {
        return try {
            val cardMap = mapOf(
                "bank" to card.bank,
                "finalCode" to card.finalCode,
                "paymentMethods" to card.paymentMethods,
                "invoiceDueDate" to card.invoiceDueDate,
                "allowsDeposit" to card.allowsDeposit,
                "closingDay" to card.closingDay,
            )
            val ref = db.collection("cards").add(cardMap).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao cadastrar cartão", e)
            Resource.Error(e.message ?: "Erro inesperado ao cadastrar cartão")
        }
    }

    override suspend fun updateCard(
        cardId: String,
        bank: String,
        paymentMethods: List<String>,
        finalCode: String,
        closingDay: Int,
        invoiceDueDate: Int,
    ): Resource<Unit> {
        return try {
            db.collection("cards").document(cardId).update(
                mapOf(
                    "bank" to bank,
                    "paymentMethods" to paymentMethods,
                    "finalCode" to finalCode,
                    "closingDay" to closingDay,
                    "invoiceDueDate" to invoiceDueDate,
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao atualizar cartão", e)
            Resource.Error(e.message ?: "Erro inesperado ao atualizar cartão")
        }
    }
}