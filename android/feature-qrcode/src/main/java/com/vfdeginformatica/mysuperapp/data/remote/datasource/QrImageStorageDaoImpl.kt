package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.vfdeginformatica.mysuperapp.domain.model.UploadedImage
import kotlinx.coroutines.tasks.await

class QrImageStorageDaoImpl(
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : QrImageStorageDao {

    override suspend fun upload(qrId: String, bytes: ByteArray): UploadedImage? {
        val uid = auth.currentUser?.uid ?: return null
        val path = "$ROOT/$uid/$qrId/${System.currentTimeMillis()}.jpg"
        return try {
            val ref = storage.reference.child(path)
            val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
            ref.putBytes(bytes, metadata).await()
            val url = ref.downloadUrl.await().toString()
            UploadedImage(url = url, path = path)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading QR image", e)
            null
        }
    }

    override suspend fun delete(path: String): Boolean {
        if (path.isBlank()) return false
        return try {
            storage.reference.child(path).delete().await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error deleting QR image $path", e)
            false
        }
    }

    companion object {
        private const val TAG = "QrImageStorageDao"
        const val ROOT = "qr_images"
    }
}
