package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.remote.dto.MuralCommentDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import kotlinx.coroutines.tasks.await

class QrCodeDaoImpl(
    private val db: FirebaseFirestore
) : QrCodeDao {

    companion object {
        private const val TAG = "QrCodeDaoImpl"
        private const val COLLECTION = "qrcodes"
    }

    override suspend fun getQrCodes(): List<QrCodeDto>? {
        return try {
            val snap = db.collection(COLLECTION).get().await()
            Log.d(TAG, "getQrCodes: ${snap.documents.size} documentos encontrados")

            val result = snap.documents.mapNotNull { doc ->
                try {
                    doc.toObject(QrCodeDto::class.java).also {
                        if (it == null) Log.w(TAG, "toObject retornou null para doc: ${doc.id}")
                        else Log.d(TAG, "Doc mapeado: id=${it.id}, redirectUrl=${it.redirectUrl}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao mapear doc ${doc.id}: ${e.message}", e)
                    null
                }
            }

            Log.d(TAG, "getQrCodes: ${result.size} QR Codes mapeados com sucesso")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar QR Codes: ${e.message}", e)
            null
        }
    }

    override suspend fun updateQrCode(id: String, qrCodeDto: QrCodeDto): Boolean {
        return try {
            db.collection(COLLECTION).document(id).set(qrCodeDto).await()
            Log.d(TAG, "updateQrCode: QR Code $id atualizado com sucesso")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar QR Code $id: ${e.message}", e)
            false
        }
    }

    override suspend fun getComments(qrCodeId: String): List<MuralCommentDto>? {
        return try {
            val snap = db.collection(COLLECTION).document(qrCodeId)
                .collection("comments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            val comments = snap.documents.mapNotNull { doc ->
                doc.toObject(MuralCommentDto::class.java)
            }
            Log.d(TAG, "getComments: ${comments.size} comentários encontrados para $qrCodeId")
            comments
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar comentários do QrCode $qrCodeId: ${e.message}", e)
            null
        }
    }

    override suspend fun deleteComment(qrCodeId: String, commentId: String): Boolean {
        return try {
            db.collection(COLLECTION).document(qrCodeId)
                .collection("comments").document(commentId)
                .delete().await()
            Log.d(TAG, "deleteComment: Comentário $commentId removido do QrCode $qrCodeId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao deletar comentário $commentId do QrCode $qrCodeId: ${e.message}", e)
            false
        }
    }
}
