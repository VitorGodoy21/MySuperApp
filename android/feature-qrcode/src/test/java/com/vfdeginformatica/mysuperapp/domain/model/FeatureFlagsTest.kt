package com.vfdeginformatica.mysuperapp.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsTest {

    @Test
    fun `sub flags only apply when parent is enabled`() {
        val parentOff = FeatureFlags(smartTags = false, qrCode = true, nfc = true, workout = false, diet = true)

        assertFalse(parentOff.isQrCodeEnabled)
        assertFalse(parentOff.isNfcEnabled)
        assertFalse(parentOff.isSmartTagsEnabled)
        assertFalse(parentOff.isDietEnabled)
    }

    @Test
    fun `smart tags is effectively disabled when both children are off`() {
        val flags = FeatureFlags(smartTags = true, qrCode = false, nfc = false)

        assertFalse(flags.isSmartTagsEnabled)
        assertTrue(FeatureFlags(qrCode = false).isSmartTagsEnabled)
    }

    @Test
    fun `hasAnyEnabled reflects effective topics`() {
        assertTrue(FeatureFlags().hasAnyEnabled)
        assertFalse(FeatureFlags(finance = false, smartTags = false, workout = false).hasAnyEnabled)
        assertFalse(FeatureFlags(finance = false, qrCode = false, nfc = false, workout = false).hasAnyEnabled)
        assertTrue(FeatureFlags(finance = false, smartTags = false, workout = true, diet = false).hasAnyEnabled)
    }
}
