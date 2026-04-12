package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.remote.dto.MuralCommentDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import kotlinx.coroutines.tasks.await

class QrCodeDaoImpl(
    private val db: FirebaseFirestore,
    private val appCheck: FirebaseAppCheck,
    private val auth: FirebaseAuth
) : QrCodeDao {

    companion object {
        private const val TAG = "QrCodeDaoImpl"
        private const val COLLECTION = "qrcodes"
    }

    override suspend fun getQrCodes(): List<QrCodeDto>? {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrEmpty()) {
            Log.w(TAG, "getQrCodes: usuário não autenticado, retornando lista vazia")
            return emptyList()
        }
        return try {
            val snap = db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get().await()
            Log.d(
                TAG,
                "getQrCodes: ${snap.documents.size} documentos encontrados para userId=$userId"
            )

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

    override suspend fun addComment(qrCodeId: String, comment: MuralCommentDto): Boolean {
        return try {
            // Obtém o token do App Check (Play Integrity em produção, debug token em dev)
            // forceRefresh = false usa o token em cache quando ainda válido
            val appCheckToken = try {
                appCheck.getAppCheckToken(false).await().token
            } catch (e: Exception) {
                Log.w(TAG, "Não foi possível obter o App Check token: ${e.message}")
                ""
            }

            val ref = db.collection(COLLECTION).document(qrCodeId)
                .collection("comments").document()
            val withIdAndToken = comment.copy(id = ref.id)
            ref.set(withIdAndToken).await()
            Log.d(
                TAG,
                "addComment: Comentário adicionado ao QrCode $qrCodeId (token presente: ${appCheckToken.isNotEmpty()})"
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao adicionar comentário ao QrCode $qrCodeId: ${e.message}", e)
            false
        }
    }
}
