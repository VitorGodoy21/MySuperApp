package com.vfdeginformatica.mysuperapp.data.local.datasource

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.featureFlagsDataStore by preferencesDataStore(name = "feature_flags")

class FeatureFlagsLocalStorageImpl(
    private val context: Context
) : FeatureFlagsLocalStorage {

    // Chaves prefixadas pelo uid para que a config de uma conta não vaze para outra.
    private fun key(uid: String, name: String) = booleanPreferencesKey("${uid}_$name")

    override fun observe(uid: String): Flow<FeatureFlags?> =
        context.featureFlagsDataStore.data.map { p ->
            val cached = p[key(uid, "cached")] ?: false
            if (!cached) return@map null
            FeatureFlags(
                finance = p[key(uid, "finance")] ?: true,
                smartTags = p[key(uid, "smartTags")] ?: true,
                qrCode = p[key(uid, "qrCode")] ?: true,
                nfc = p[key(uid, "nfc")] ?: true,
                workout = p[key(uid, "workout")] ?: true,
                diet = p[key(uid, "diet")] ?: true,
            )
        }

    override suspend fun save(uid: String, flags: FeatureFlags) {
        context.featureFlagsDataStore.edit { p ->
            p[key(uid, "cached")] = true
            p[key(uid, "finance")] = flags.finance
            p[key(uid, "smartTags")] = flags.smartTags
            p[key(uid, "qrCode")] = flags.qrCode
            p[key(uid, "nfc")] = flags.nfc
            p[key(uid, "workout")] = flags.workout
            p[key(uid, "diet")] = flags.diet
        }
    }

    override suspend fun clear() {
        context.featureFlagsDataStore.edit { it.clear() }
    }
}
