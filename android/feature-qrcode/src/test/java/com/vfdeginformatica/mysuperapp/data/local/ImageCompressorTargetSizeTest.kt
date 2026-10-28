package com.vfdeginformatica.mysuperapp.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageCompressorTargetSizeTest {

    @Test
    fun `keeps dimensions when within limit`() {
        assertEquals(800 to 600, ImageCompressor.targetSize(800, 600, 1600))
        assertEquals(1600 to 900, ImageCompressor.targetSize(1600, 900, 1600))
    }

    @Test
    fun `scales landscape proportionally by the larger side`() {
        assertEquals(1600 to 1200, ImageCompressor.targetSize(4000, 3000, 1600))
    }

    @Test
    fun `scales portrait proportionally by the larger side`() {
        assertEquals(900 to 1600, ImageCompressor.targetSize(1800, 3200, 1600))
    }

    @Test
    fun `never produces zero sized dimensions`() {
        assertEquals(1 to 1600, ImageCompressor.targetSize(1, 100000, 1600))
        assertEquals(1 to 1, ImageCompressor.targetSize(0, 0, 1600))
    }
}
