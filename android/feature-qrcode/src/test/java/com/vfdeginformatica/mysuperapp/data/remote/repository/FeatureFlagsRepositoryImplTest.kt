package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.local.datasource.FeatureFlagsLocalStorage
import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.data.remote.datasource.UserRemoteDao
import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings
import com.vfdeginformatica.mysuperapp.domain.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsRepositoryImplTest {

    private class FakeLocalStorage : FeatureFlagsLocalStorage {
        val cache = MutableStateFlow<Map<String, FeatureFlags>>(emptyMap())
        override fun observe(uid: String): Flow<FeatureFlags?> = cache.map { it[uid] }
        override suspend fun save(uid: String, flags: FeatureFlags) { cache.value = cache.value + (uid to flags) }
        override suspend fun clear() { cache.value = emptyMap() }
    }

    private class FakeSession(session: UserSession) : UserSessionSecureStorage {
        private val state = MutableStateFlow(session)
        override val sessionFlow: Flow<UserSession> = state
        override suspend fun getUserSession(): UserSession = state.value
        override suspend fun write(s: UserSession) { state.value = s }
        override suspend fun clear() { state.value = UserSession() }
    }

    private class FakeRemote(private val result: () -> FeatureFlags) : UserRemoteDao {
        override suspend fun getFeatureFlags(uid: String): FeatureFlags = result()
        override suspend fun createUser(uid: String, email: String, name: String) = Unit
        override suspend fun getUserSession(id: String): UserSession? = null
        override suspend fun updateUser(uid: String, name: String, email: String) = Unit
        override suspend fun getNotificationSettings(uid: String): NotificationSettings = NotificationSettings()
        override suspend fun updateNotificationSettings(uid: String, settings: NotificationSettings) = Unit
        override suspend fun registerFcmToken(uid: String, token: String) = Unit
        override suspend fun unregisterFcmToken(uid: String, token: String) = Unit
    }

    private val loggedIn = UserSession(id = "u1", isLoggedIn = true)
    private val remoteFlags = FeatureFlags(finance = false, diet = false)

    @Test
    fun `observe without cache emits defaults`() = runTest {
        val repo = FeatureFlagsRepositoryImpl(FakeRemote { remoteFlags }, FakeLocalStorage(), FakeSession(loggedIn))

        assertEquals(FeatureFlags(), repo.observe().first())
    }

    @Test
    fun `refresh stores remote flags in the cache for the logged user`() = runTest {
        val local = FakeLocalStorage()
        val repo = FeatureFlagsRepositoryImpl(FakeRemote { remoteFlags }, local, FakeSession(loggedIn))

        val result = repo.refresh()

        assertTrue(result is Resource.Success)
        assertEquals(remoteFlags, local.cache.value["u1"])
        assertEquals(remoteFlags, repo.observe().first())
    }

    @Test
    fun `remote failure keeps the cached flags`() = runTest {
        val local = FakeLocalStorage().apply { save("u1", remoteFlags) }
        val repo = FeatureFlagsRepositoryImpl(FakeRemote { error("offline") }, local, FakeSession(loggedIn))

        val result = repo.refresh()

        assertTrue(result is Resource.Error)
        assertEquals(remoteFlags, repo.observe().first())
    }

    @Test
    fun `logged out user gets defaults and cannot refresh`() = runTest {
        val local = FakeLocalStorage().apply { save("u1", remoteFlags) }
        val repo = FeatureFlagsRepositoryImpl(FakeRemote { remoteFlags }, local, FakeSession(UserSession()))

        assertEquals(FeatureFlags(), repo.observe().first())
        assertTrue(repo.refresh() is Resource.Error)
    }
}
