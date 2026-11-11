package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyTarget
import com.vfdeginformatica.mysuperapp.domain.model.toDto
import com.vfdeginformatica.mysuperapp.domain.model.withSpotifyTarget
import com.vfdeginformatica.mysuperapp.testutil.NoopQrCodeGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrCodeMapperTest {

    private val generator = NoopQrCodeGenerator

    @Test
    fun `IMAGE dto maps image fields and caption`() {
        val dto = QrCodeDto(
            id = "qr1",
            identifier = "Foto",
            type = "IMAGE",
            text = "Legenda",
            imageUrl = "https://storage/img.jpg",
            imagePath = "qr_images/u1/qr1/1.jpg"
        )

        val model = dto.toModel(generator)

        assertEquals(QrCodeType.IMAGE, model.type)
        assertEquals("Legenda", model.text)
        assertEquals("https://storage/img.jpg", model.imageUrl)
        assertEquals("qr_images/u1/qr1/1.jpg", model.imagePath)
    }

    @Test
    fun `model to dto keeps image fields`() {
        val model = QrCode(
            id = "qr1",
            redirectUrl = "",
            type = QrCodeType.IMAGE,
            text = "Legenda",
            imageUrl = "https://storage/img.jpg",
            imagePath = "qr_images/u1/qr1/1.jpg"
        )

        val dto = model.toDto()

        assertEquals("IMAGE", dto.type)
        assertEquals("https://storage/img.jpg", dto.imageUrl)
        assertEquals("qr_images/u1/qr1/1.jpg", dto.imagePath)
        assertEquals(model, dto.toModel(generator))
    }

    @Test
    fun `unknown type falls back to REDIRECT and empty image fields`() {
        val model = QrCodeDto(id = "qr1", type = "VIDEO").toModel(generator)

        assertEquals(QrCodeType.REDIRECT, model.type)
        assertEquals("", model.imageUrl)
        assertEquals("", model.imagePath)
    }

    @Test
    fun `SPOTIFY round trip keeps the target and shuffle`() {
        val target = SpotifyTarget(
            uri = "spotify:playlist:abc",
            name = "Chill",
            subtitle = "Vitor",
            imageUrl = "https://i.scdn.co/cover.jpg",
            deviceId = "dev1",
            deviceName = "Todo lugar",
            shuffle = true
        )
        val model = QrCode(id = "qr1", redirectUrl = "", type = QrCodeType.SPOTIFY)
            .withSpotifyTarget(target)

        val dto = model.toDto()
        val restored = dto.toModel(generator)

        assertEquals("SPOTIFY", dto.type)
        assertEquals("spotify:playlist:abc", dto.spotifyUri)
        assertEquals(true, dto.spotifyShuffle)
        assertEquals(QrCodeType.SPOTIFY, restored.type)
        assertEquals(target, restored.spotifyTarget)
    }

    @Test
    fun `SPOTIFY without uri has no target`() {
        val model = QrCodeDto(id = "qr1", type = "SPOTIFY", spotifyDeviceName = "Todo lugar").toModel(generator)

        assertEquals(QrCodeType.SPOTIFY, model.type)
        assertNull(model.spotifyTarget)
    }
}
