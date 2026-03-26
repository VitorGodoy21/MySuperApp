package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import kotlinx.coroutines.tasks.await

class TransactionDaoImpl(
    private val db: FirebaseFirestore,
) : TransactionDao {
    override suspend fun addTransaction(transaction: TransactionDto): Resource<Unit> {
        val transactionMap = mapOf(
            "title" to transaction.title,
            "description" to transaction.description,
            "transactionType" to transaction.transactionType,
            "paymentMethod" to transaction.paymentMethod,
            "amount" to transaction.amount,
            "isRecurring" to transaction.isRecurring,
            "transactionDate" to transaction.transactionDate,
            "category" to transaction.category,
            "status" to transaction.status,
            "cardId" to transaction.cardId,
            "invoiceMonth" to transaction.invoiceMonth,
            "installmentGroupId" to transaction.installmentGroupId,
        )

        return try {
            db.collection("transactions")
                .add(transactionMap)
                .await()
            Log.d("Firebase", "Documento salvo com sucesso no Firestore")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao salvar dados", e)
            Resource.Error(e.message ?: "Erro inesperado ao salvar transação")
        }
    }
}