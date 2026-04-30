package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeAccessLogDto
import kotlinx.coroutines.tasks.await

class QrCodeAccessLogDaoImpl(
    private val db: FirebaseFirestore
) : QrCodeAccessLogDao {

    override suspend fun saveAccessLog(accessLog: QrCodeAccessLogDto): Boolean {
        return try {
            db.collection("qrcodes").document(accessLog.qrCodeId)
                .collection("access_logs").add(accessLog).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAccessLogsByQrCode(qrCodeId: String): List<QrCodeAccessLogDto>? {
        return try {
            val snap = db.collection("qrcodes").document(qrCodeId)
                .collection("access_logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1000)
                .get().await()
            snap.documents.mapNotNull { doc ->
                doc.toObject(QrCodeAccessLogDto::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getAccessLogStatistics(qrCodeId: String): Map<String, Any>? {
        return try {
            val logs = getAccessLogsByQrCode(qrCodeId) ?: return null

            val totalAccess = logs.size
            val cityCounts = mutableMapOf<String, Int>()
            val countryCounts = mutableMapOf<String, Int>()

            logs.forEach { log ->
                if (log.city.isNotEmpty()) {
                    cityCounts[log.city] = (cityCounts[log.city] ?: 0) + 1
                }
                if (log.country.isNotEmpty()) {
                    countryCounts[log.country] = (countryCounts[log.country] ?: 0) + 1
                }
            }

            val topCity = cityCounts.maxByOrNull { it.value }
            val topCountry = countryCounts.maxByOrNull { it.value }

            mapOf(
                "totalAccess" to totalAccess,
                "topCity" to (topCity?.key ?: "Desconhecido"),
                "topCityCount" to (topCity?.value ?: 0),
                "topCountry" to (topCountry?.key ?: "Desconhecido"),
                "topCountryCount" to (topCountry?.value ?: 0),
                "uniqueCities" to cityCounts.size,
                "uniqueCountries" to countryCounts.size
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteAccessLog(qrCodeId: String, logId: String): Boolean {
        return try {
            db.collection("qrcodes").document(qrCodeId)
                .collection("access_logs").document(logId)
                .delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}

