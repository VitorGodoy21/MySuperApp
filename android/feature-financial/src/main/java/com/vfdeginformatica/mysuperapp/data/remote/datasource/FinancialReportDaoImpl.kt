package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.FinancialReportDto
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FinancialReportDaoImpl(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : FinancialReportDao {

    private companion object {
        const val TAG = "FinancialReportDao"
        const val CALLABLE = "generateFinancialReport"
        const val CALLABLE_AI = "generateFinancialReportAi"
        const val SUBCOLLECTION = "financial_reports"
    }

    private fun currentUserId(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

    private fun reports(userId: String) =
        db.collection("users").document(userId).collection(SUBCOLLECTION)

    override suspend fun getReport(month: String): FinancialReportDto? {
        val userId = currentUserId() ?: return null
        val snap = reports(userId).document(month).get().await()
        val data = snap.data ?: return null
        return FinancialReportDto(snap.id, data)
    }

    override suspend fun listReports(): List<FinancialReportDto> {
        val userId = currentUserId() ?: return emptyList()
        // Sem orderBy(__name__, DESC): exigiria índice; ids yyyy-MM ordenam lexicograficamente.
        val snap = reports(userId).get().await()
        return snap.documents
            .mapNotNull { doc -> doc.data?.let { FinancialReportDto(doc.id, it) } }
            .sortedByDescending { it.id }
    }

    override suspend fun requestReport(month: String?): Resource<String> {
        if (currentUserId() == null) return Resource.Error("Entre na sua conta para gerar o relatório")
        val payload = if (month != null) mapOf("month" to month) else emptyMap()
        return call(CALLABLE, payload, "Não foi possível gerar o relatório") { result ->
            result?.get("month") as? String
        }
    }

    override suspend fun requestAiNarrative(month: String): Resource<Unit> {
        if (currentUserId() == null) return Resource.Error("Entre na sua conta para pedir a análise")
        return call(CALLABLE_AI, mapOf("month" to month), "Não foi possível gerar a análise com IA", timeoutSeconds = 120) { Unit }
    }

    /** Invoca um callable mapeando erros do Functions para mensagens amigáveis. */
    private suspend fun <T> call(
        name: String,
        payload: Map<String, Any>,
        genericError: String,
        timeoutSeconds: Long = 70,
        parse: (Map<*, *>?) -> T?,
    ): Resource<T> = try {
        val callable = functions.getHttpsCallable(name).apply { setTimeout(timeoutSeconds, TimeUnit.SECONDS) }
        val result = callable.call(payload).await().data as? Map<*, *>
        parse(result)?.let { Resource.Success(it) } ?: Resource.Error("Resposta inesperada do servidor")
    } catch (e: FirebaseFunctionsException) {
        Log.w(TAG, "$name failed: ${e.code} ${e.message}")
        Resource.Error(
            when (e.code) {
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED,
                FirebaseFunctionsException.Code.FAILED_PRECONDITION,
                FirebaseFunctionsException.Code.INVALID_ARGUMENT,
                FirebaseFunctionsException.Code.UNAVAILABLE,
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> e.message ?: genericError
                else -> "$genericError agora. Tente novamente."
            }
        )
    } catch (e: Exception) {
        Log.e(TAG, "$name failed", e)
        Resource.Error("$genericError agora. Tente novamente.")
    }
}
