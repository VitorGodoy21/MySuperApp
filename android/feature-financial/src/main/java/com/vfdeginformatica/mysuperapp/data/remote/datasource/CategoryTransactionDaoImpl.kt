package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import kotlinx.coroutines.tasks.await

class CategoryTransactionDaoImpl(
    private val db: FirebaseFirestore
) : CategoryTransactionDao {

    private companion object {
        const val COLLECTION = "transactions_category"
    }

    override suspend fun getCategoriesTransaction(): List<CategoryTransactionDto> {
        val snap = db.collection(COLLECTION).get().await()
        return snap.documents.mapNotNull { doc ->
            doc.toObject(CategoryTransactionDto::class.java)
        }
    }

    override suspend fun addCategory(category: CategoryTransactionDto): Resource<String> {
        return try {
            val data = mapOf(
                "title" to category.title,
                "description" to category.description,
                "transactionType" to category.transactionType,
            )
            val ref = db.collection(COLLECTION).add(data).await()
            Resource.Success(ref.id)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao criar categoria", e)
            Resource.Error(e.message ?: "Erro inesperado ao criar categoria")
        }
    }

    override suspend fun updateCategory(
        id: String,
        title: String,
        description: String,
        transactionType: String,
    ): Resource<Unit> {
        return try {
            db.collection(COLLECTION).document(id).update(
                mapOf(
                    "title" to title,
                    "description" to description,
                    "transactionType" to transactionType,
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao atualizar categoria $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao atualizar categoria")
        }
    }

    override suspend fun deleteCategory(id: String): Resource<Unit> {
        return try {
            db.collection(COLLECTION).document(id).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("Firebase", "Erro ao excluir categoria $id", e)
            Resource.Error(e.message ?: "Erro inesperado ao excluir categoria")
        }
    }
}