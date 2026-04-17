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
        val snapshot = userRef.get().await()
        val userSession = UserSession(
            id = id,
            email = snapshot.getString("email") ?: "",
            isLoggedIn = true,
            lastSignInAt = System.currentTimeMillis(),
            name = snapshot.getString("name") ?: "",
            isAdmin = snapshot.getBoolean("isAdmin") ?: false
        )
        return userSession
    }

    override suspend fun updateUser(uid: String, name: String, email: String) {
        db.collection("users").document(uid)
            .update(mapOf("name" to name, "email" to email))
            .await()
        Log.d("UserRemoteDaoImpl", "updateUser: uid=$uid name=$name email=$email")
    }
}