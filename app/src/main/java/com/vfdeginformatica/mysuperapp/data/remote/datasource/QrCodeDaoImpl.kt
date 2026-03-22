package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import kotlinx.coroutines.tasks.await

class QrCodeDaoImpl(
    private val db: FirebaseFirestore
) : QrCodeDao {
    override suspend fun getQrCodes(): List<QrCodeDto>? {
        return try {
            val snap = db.collection("qrcodes").get().await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(QrCodeDto::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }
}
