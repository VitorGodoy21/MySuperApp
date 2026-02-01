package com.vfdeginformatica.mysuperapp.data.remote.datasource

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.domain.model.UserSession
import kotlinx.coroutines.tasks.await

class UserRemoteDaoImpl(
    private val db: FirebaseFirestore
) : UserRemoteDao {
    override suspend fun createUser(
        uid: String,
        email: String,
        name: String
    ) {
        val userMap = mapOf(
            "email" to email,
            "createdAt" to FieldValue.serverTimestamp(),
            "name" to name
        )

        db.collection("users")
            .document(uid) // UID como ID do documento
            .set(userMap)
            .addOnSuccessListener {
                Log.d("Firebase", "Usuário criado e documento salvo no Firestore")
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Erro ao salvar dados do usuário", e)
            }
    }

    override suspend fun getUserSession(id: String): UserSession {
        val userRef = db.collection("users").document(id)
        val userSession = UserSession(
            id = id,
            email = userRef.get().await().getString("email") ?: "",
            isLoggedIn = true,
            lastSignInAt = System.currentTimeMillis(),
            name = userRef.get().await().getString("name") ?: ""
        )

        return userSession
    }

}