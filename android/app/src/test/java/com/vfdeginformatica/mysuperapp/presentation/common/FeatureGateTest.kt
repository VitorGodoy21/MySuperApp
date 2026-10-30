package com.vfdeginformatica.mysuperapp.presentation.common

import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import com.vfdeginformatica.mysuperapp.nfc.navigation.SharedNfcRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureGateTest {

    @Test
    fun mapsRoutePatternsAndConcreteRoutesToFeatures() {
        assertEquals(GatedFeature.FINANCE, FeatureGate.featureOf(Screen.FinancialScreen.route))
        assertEquals(GatedFeature.FINANCE, FeatureGate.featureOf(Screen.CardDetailScreen.route))
        assertEquals(GatedFeature.FINANCE, FeatureGate.featureOf(Screen.NewTransactionScreen.createRoute("EXPENSE")))
        assertEquals(GatedFeature.QR_CODE, FeatureGate.featureOf(Screen.QrCodeListScreen.route))
        assertEquals(GatedFeature.NFC, FeatureGate.featureOf(SharedNfcRoutes.NFC_SCREEN))
        assertEquals(GatedFeature.WORKOUT, FeatureGate.featureOf(Screen.WorkoutViewScreen.createRoute("w1")))
        assertEquals(GatedFeature.DIET, FeatureGate.featureOf(Screen.DietViewScreen.route))
    }

    @Test
    fun unmappedRoutesAreAlwaysAllowed() {
        assertNull(FeatureGate.featureOf(Screen.HomeScreen.route))
        assertNull(FeatureGate.featureOf(Screen.SettingsScreen.route))
        assertNull(FeatureGate.featureOf(null))
        assertTrue(FeatureFlags(finance = false, smartTags = false, workout = false).allows(Screen.HomeScreen.route))
    }

    @Test
    fun blocksRoutesOfDisabledFeaturesOnly() {
        val flags = FeatureFlags(finance = false, qrCode = false, diet = false)

        assertEquals(GatedFeature.FINANCE, FeatureGate.blockedBy(flags, Screen.CategoryListScreen.route))
        assertEquals(GatedFeature.QR_CODE, FeatureGate.blockedBy(flags, Screen.QrCodeScreen.route))
        assertNull(FeatureGate.blockedBy(flags, SharedNfcRoutes.NFC_SCREEN))
        assertEquals(GatedFeature.DIET, FeatureGate.blockedBy(flags, Screen.DietDetailScreen.route))
        assertTrue(flags.allows(Screen.WorkoutHubScreen.route))
    }

    @Test
    fun disablingParentBlocksChildren() {
        val flags = FeatureFlags(smartTags = false, workout = false)

        assertFalse(flags.allows(SharedNfcRoutes.NFC_SCREEN))
        assertFalse(flags.allows(Screen.QrCodeListScreen.route))
        assertFalse(flags.allows(Screen.DietViewScreen.route))
        assertFalse(flags.allows(Screen.ExerciseListScreen.route))
    }
}
