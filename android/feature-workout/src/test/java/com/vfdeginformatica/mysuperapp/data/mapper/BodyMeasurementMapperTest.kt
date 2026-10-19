package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BodyMeasurementMapperTest {

    @Test
    fun `measuredAt survives LocalDate to Timestamp round trip and bmi is computed`() {
        val original = BodyMeasurement(
            id = "m1",
            weightKg = 72.5,
            heightCm = 180.0,
            measuredAt = LocalDate.of(2026, 3, 15),
            notes = "manhã",
        )

        val restored = original.toDto().toModel()

        assertEquals(original, restored)
        assertEquals(72.5 / (1.8 * 1.8), restored.bmi!!, 0.0001)
    }
}
