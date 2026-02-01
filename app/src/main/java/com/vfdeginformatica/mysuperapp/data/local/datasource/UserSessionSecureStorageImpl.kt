package com.vfdeginformatica.mysuperapp.data.local.datasource

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vfdeginformatica.mysuperapp.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val DS_NAME = "user_session"
val Context.userSessionDataStore by preferencesDataStore(name = DS_NAME)

private object UserSessionConstants {
    const val PREFERENCE_UID = "uid"
    const val PREFERENCE_EMAIL = "email"
    const val PREFERENCE_IS_LOGGED_IN = "is_logged_in"
    const val PREFERENCE_LAST_SIGN_IN_AT = "last_sign_in_at"
    const val PREFERENCE_NAME = "name"
}

private object UserSessionObject {
    val UID = stringPreferencesKey(UserSessionConstants.PREFERENCE_UID)
    val EMAIL = stringPreferencesKey(UserSessionConstants.PREFERENCE_EMAIL)
    val LOGGED = booleanPreferencesKey(UserSessionConstants.PREFERENCE_IS_LOGGED_IN)
    val LAST = longPreferencesKey(UserSessionConstants.PREFERENCE_LAST_SIGN_IN_AT)
    val NAME = stringPreferencesKey(UserSessionConstants.PREFERENCE_NAME)
}

class UserSessionSecureStorageImpl(
    private val context: Context
) : UserSessionSecureStorage {

    override val sessionFlow: Flow<UserSession> = context.userSessionDataStore.data.map { p ->
        UserSession(
            id = p[UserSessionObject.UID].orEmpty(),
            email = p[UserSessionObject.EMAIL].orEmpty(),
            isLoggedIn = p[UserSessionObject.LOGGED] ?: false,
            lastSignInAt = p[UserSessionObject.LAST] ?: 0L,
            name = p[UserSessionObject.NAME].orEmpty(),
        )
    }

    override suspend fun getUserSession(): UserSession {
        val prefs = context.userSessionDataStore.data.first()
        val uid = prefs[UserSessionObject.UID].orEmpty()
        val email = prefs[UserSessionObject.EMAIL].orEmpty()
        val isLoggedIn = prefs[UserSessionObject.LOGGED] ?: false
        val lastSignInAt = prefs[UserSessionObject.LAST] ?: 0L
        val name = prefs[UserSessionObject.NAME].orEmpty()

        return UserSession(
            id = uid,
            email = email,
            isLoggedIn = isLoggedIn,
            lastSignInAt = lastSignInAt,
            name = name
        )
    }

    override suspend fun write(s: UserSession) {
        context.userSessionDataStore.edit { user ->
            user[UserSessionObject.UID] = s.id
            user[UserSessionObject.EMAIL] = s.email
            user[UserSessionObject.LOGGED] = s.isLoggedIn
            user[UserSessionObject.LAST] = s.lastSignInAt
            user[UserSessionObject.NAME] = s.name
        }
    }

    override suspend fun clear() {
        context.userSessionDataStore.edit { it.clear() }
    }

}