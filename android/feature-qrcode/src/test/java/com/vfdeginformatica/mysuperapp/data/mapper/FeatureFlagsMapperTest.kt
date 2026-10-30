package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureFlagsMapperTest {

    @Test
    fun `null map enables everything`() {
        assertEquals(FeatureFlags(), (null as Map<*, *>?).toFeatureFlags())
    }

    @Test
    fun `missing or non boolean keys default to true`() {
        val flags = mapOf("finance" to false, "nfc" to "no", "workout" to 0).toFeatureFlags()

        assertEquals(FeatureFlags(finance = false), flags)
    }

    @Test
    fun `round trips through firestore map`() {
        val original = FeatureFlags(finance = false, smartTags = true, qrCode = false, nfc = true, workout = false, diet = true)

        assertEquals(original, original.toFirestoreMap().toFeatureFlags())
    }
}
