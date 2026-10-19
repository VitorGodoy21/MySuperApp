package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import kotlinx.coroutines.tasks.await

class TransactionDaoImpl(
    private val db: FirebaseFirestore,
) : TransactionDao {

    private companion object {
        const val MAX_BATCH_OPERATIONS = 500
    }

    private fun TransactionDto.toMap(): Map<String, Any?> = mapOf(
        "title" to title,
        "description" to description,
        "transactionType" to transactionType,
        "paymentMethod" to paymentMethod,
        "amount" to amount,
        "isRecurring" to isRecurring,
        "transactionDate" to transactionDate,
        "category" to category,
        "status" to status,
        "cardId" to cardId,
        "invoiceMonth" to invoiceMonth,
        "installmentGroupId" to installmentGroupId,
        "installments" to installments,
        "installmentNumber" to installmentNumber,
        "recurrenceStart" to recurrenceStart,
        "recurrenceEnd" to recurrenceEnd,
        "excludedMonths" to excludedMonths,
        "overrides" to overrides,
    )

    override suspend fun addTransaction(transaction: TransactionDto): Resource<Unit> {
        return try {
            db.collection("transactions")
                .add(transaction.toMap())
                .await()
            Log.d("Firebase", "Documento salvo com sucesso no Firestore")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao salvar dados", e)
            Resource.Error(e.message ?: "Erro inesperado ao salvar transação")
        }
    }

    override suspend fun addTransactions(transactions: List<TransactionDto>): Resource<Unit> {
        return try {
            val batch = db.batch()
            val collection = db.collection("transactions")
            transactions.forEach { transaction ->
                batch.set(collection.document(), transaction.toMap())
            }
            batch.commit().await()
            Log.d("Firebase", "${transactions.size} transações salvas em batch")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao salvar transações em batch", e)
            Resource.Error(e.message ?: "Erro inesperado ao salvar transações")
        }
    }

    override suspend fun updateTransaction(
        id: String,
        title: String,
        amount: Double,
        category: List<String>
    ): Resource<Unit> {
        return try {
            db.collection("transactions").document(id)
                .update(mapOf("title" to title, "amount" to amount, "category" to category))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao atualizar transação $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao atualizar transação")
        }
    }

    override suspend fun updateTransactionCategories(
        id: String,
        category: List<String>
    ): Resource<Unit> {
        return try {
            db.collection("transactions").document(id)
                .update("category", category)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao atualizar categorias da transação $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao atualizar categorias")
        }
    }

    override suspend fun updateCategoriesForInstallmentGroup(
        groupId: String,
        category: List<String>
    ): Resource<Unit> {
        return try {
            val snap = db.collection("transactions")
                .whereEqualTo("installmentGroupId", groupId)
                .get().await()
            snap.documents.chunked(MAX_BATCH_OPERATIONS).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc -> batch.update(doc.reference, "category", category) }
                batch.commit().await()
            }
            Log.d("Firebase", "Categorias aplicadas a ${snap.size()} parcelas do grupo $groupId")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao atualizar categorias do grupo $groupId", e)
            Resource.Error(e.message ?: "Erro inesperado ao atualizar categorias das parcelas")
        }
    }

    override suspend fun deleteTransaction(id: String): Resource<Unit> {
        return try {
            db.collection("transactions").document(id)
                .delete()
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao excluir transação $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao excluir transação")
        }
    }

    override suspend fun removeCategoryFromTransactions(categoryId: String): Resource<Unit> {
        return try {
            val snap = db.collection("transactions")
                .whereArrayContains("category", categoryId)
                .get().await()
            // WriteBatch aceita no máximo 500 operações por commit
            snap.documents.chunked(MAX_BATCH_OPERATIONS).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { doc ->
                    batch.update(doc.reference, "category", FieldValue.arrayRemove(categoryId))
                }
                batch.commit().await()
            }
            Log.d("Firebase", "Categoria $categoryId removida de ${snap.size()} transações")
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao remover categoria $categoryId das transações", e)
            Resource.Error(e.message ?: "Erro inesperado ao remover categoria das transações")
        }
    }

    override suspend fun getTransactions(
        start: Timestamp,
        end: Timestamp
    ): Resource<List<TransactionDto>> {
        return try {
            val snap = db.collection("transactions")
                .whereGreaterThanOrEqualTo("transactionDate", start)
                .whereLessThan("transactionDate", end)
                .orderBy("transactionDate", Query.Direction.DESCENDING)
                .get().await()
            val transactions = snap.documents.mapNotNull { doc ->
                try {
                    doc.toObject(TransactionDto::class.java)
                } catch (e: Exception) {
                    Log.e("Firebase", "Erro ao mapear transação ${doc.id}", e)
                    null
                }
            }
            Resource.Success(transactions)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao buscar transações", e)
            Resource.Error(e.message ?: "Erro inesperado ao buscar transações")
        }
    }

    override suspend fun getTransactionsByInvoiceMonth(
        invoiceMonth: String
    ): Resource<List<TransactionDto>> {
        return try {
            val snap = db.collection("transactions")
                .whereEqualTo("invoiceMonth", invoiceMonth)
                .get().await()
            Resource.Success(snap.documents.mapNotNull { doc ->
                try {
                    doc.toObject(TransactionDto::class.java)
                } catch (e: Exception) {
                    Log.e("Firebase", "Erro ao mapear transação ${doc.id}", e)
                    null
                }
            })
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao buscar fatura de $invoiceMonth", e)
            Resource.Error(e.message ?: "Erro inesperado ao buscar fatura")
        }
    }

    override suspend fun getRecurringTransactions(): Resource<List<TransactionDto>> {
        return try {
            val snap = db.collection("transactions")
                .whereEqualTo("isRecurring", true)
                .get().await()
            Resource.Success(snap.documents.mapNotNull { doc ->
                try {
                    doc.toObject(TransactionDto::class.java)
                } catch (e: Exception) {
                    Log.e("Firebase", "Erro ao mapear transação ${doc.id}", e)
                    null
                }
            })
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao buscar transações recorrentes", e)
            Resource.Error(e.message ?: "Erro inesperado ao buscar recorrentes")
        }
    }

    override suspend fun excludeRecurrenceMonth(id: String, month: String): Resource<Unit> {
        return try {
            db.collection("transactions").document(id)
                .update("excludedMonths", FieldValue.arrayUnion(month))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao excluir ocorrência $month da transação $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao excluir ocorrência")
        }
    }

    override suspend fun endRecurrence(id: String, lastMonth: String): Resource<Unit> {
        return try {
            db.collection("transactions").document(id)
                .update("recurrenceEnd", lastMonth)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao encerrar recorrência $id em $lastMonth", e)
            Resource.Error(e.message ?: "Erro inesperado ao encerrar recorrência")
        }
    }

    override suspend fun overrideRecurrenceMonth(
        id: String,
        month: String,
        title: String,
        amount: Double
    ): Resource<Unit> {
        return try {
            db.collection("transactions").document(id)
                .update(
                    FieldPath.of("overrides", month),
                    mapOf("amount" to amount, "title" to title)
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao ajustar ocorrência $month da transação $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao ajustar ocorrência")
        }
    }

    override suspend fun updateRecurrenceTemplate(
        id: String,
        title: String,
        amount: Double,
        category: List<String>,
        excludedMonths: List<String>,
        overrides: Map<String, Map<String, Any>>
    ): Resource<Unit> {
        return try {
            db.collection("transactions").document(id)
                .update(
                    mapOf(
                        "title" to title,
                        "amount" to amount,
                        "category" to category,
                        "excludedMonths" to excludedMonths,
                        "overrides" to overrides,
                    )
                )
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao atualizar recorrência $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao atualizar recorrência")
        }
    }

    override suspend fun splitRecurrence(
        originalId: String,
        lastMonthOfOriginal: String,
        newTransaction: TransactionDto,
        originalCategory: List<String>
    ): Resource<Unit> {
        return try {
            val collection = db.collection("transactions")
            val batch = db.batch()
            batch.update(
                collection.document(originalId),
                mapOf("recurrenceEnd" to lastMonthOfOriginal, "category" to originalCategory)
            )
            batch.set(collection.document(), newTransaction.toMap())
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao dividir recorrência $originalId em $lastMonthOfOriginal", e)
            Resource.Error(e.message ?: "Erro inesperado ao atualizar recorrência")
        }
    }
}