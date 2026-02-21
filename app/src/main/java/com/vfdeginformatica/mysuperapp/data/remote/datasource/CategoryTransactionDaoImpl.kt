package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import kotlinx.coroutines.tasks.await

class CategoryTransactionDaoImpl(
    private val db: FirebaseFirestore
) : CategoryTransactionDao {
    override suspend fun getCategoriesTransaction(): List<CategoryTransactionDto> {
        val snap = db.collection("transactions_category").get().await()
        return snap.documents.mapNotNull { doc ->
            doc.toObject(CategoryTransactionDto::class.java)
        }
    }
}