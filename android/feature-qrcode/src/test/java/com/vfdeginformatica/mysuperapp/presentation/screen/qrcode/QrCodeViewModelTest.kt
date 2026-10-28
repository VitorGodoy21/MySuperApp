package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode

import android.net.Uri
import com.vfdeginformatica.mysuperapp.data.local.ImageCompressor
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.DeleteQrImageUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UpdateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UploadQrImageUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEvent
import com.vfdeginformatica.mysuperapp.testutil.FakeQrCodeRepository
import com.vfdeginformatica.mysuperapp.testutil.FakeQrImageRepository
import com.vfdeginformatica.mysuperapp.testutil.NoopQrCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class QrCodeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val qrRepository = FakeQrCodeRepository()
    private val imageRepository = FakeQrImageRepository()
    private val compressor: ImageCompressor = mock()
    private val pickedUri: Uri = mock()
    private val bytes = byteArrayOf(1, 2, 3)

    private lateinit var viewModel: QrCodeViewModel

    private val imageQr = QrCode(
        id = "qr1",
        identifier = "Foto",
        redirectUrl = "",
        staticUrl = "https://baila.space/qr/?id=qr1",
        type = QrCodeType.IMAGE,
        text = "Legenda",
        imageUrl = "https://storage/old.jpg",
        imagePath = "qr_images/u1/qr1/old.jpg"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = QrCodeViewModel(
            updateQrCodeUseCase = UpdateQrCodeUseCase(qrRepository),
            uploadQrImageUseCase = UploadQrImageUseCase(imageRepository),
            deleteQrImageUseCase = DeleteQrImageUseCase(imageRepository),
            imageCompressor = compressor,
            qrCodeGenerator = NoopQrCodeGenerator,
            bitmapDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun load(qrCode: QrCode) = runTest(testDispatcher) {
        viewModel.onEvent(QrCodeEvent.OnQrCodeLoaded(qrCode))
        advanceUntilIdle()
    }

    @Test
    fun `switching TEXT to IMAGE keeps text as caption and clears url`() = runTest(testDispatcher) {
        load(imageQr.copy(type = QrCodeType.TEXT, imageUrl = "", imagePath = "", redirectUrl = "https://x"))

        viewModel.onEvent(QrCodeEvent.OnTypeChanged(QrCodeType.IMAGE))

        val qr = viewModel.uiState.value.qrCode!!
        assertEquals(QrCodeType.IMAGE, qr.type)
        assertEquals("Legenda", qr.text)
        assertEquals("", qr.redirectUrl)
    }

    @Test
    fun `leaving IMAGE clears image fields and schedules deletion only after save`() = runTest(testDispatcher) {
        load(imageQr)

        viewModel.onEvent(QrCodeEvent.OnTypeChanged(QrCodeType.MURAL))

        val state = viewModel.uiState.value
        assertEquals("", state.qrCode!!.imageUrl)
        assertEquals("", state.qrCode!!.imagePath)
        assertEquals("qr_images/u1/qr1/old.jpg", state.pendingDeletePath)
        assertTrue(imageRepository.deleted.isEmpty())

        viewModel.onEvent(QrCodeEvent.OnSaveQrCode)
        advanceUntilIdle()

        assertEquals(listOf("qr_images/u1/qr1/old.jpg"), imageRepository.deleted)
        assertEquals("", viewModel.uiState.value.pendingDeletePath)
        assertEquals(QrCodeType.MURAL, qrRepository.updates.single().second.type)
    }

    @Test
    fun `returning to IMAGE before saving restores the previous image`() = runTest(testDispatcher) {
        load(imageQr)

        viewModel.onEvent(QrCodeEvent.OnTypeChanged(QrCodeType.TEXT))
        viewModel.onEvent(QrCodeEvent.OnTypeChanged(QrCodeType.IMAGE))

        val state = viewModel.uiState.value
        assertEquals(imageQr.imageUrl, state.qrCode!!.imageUrl)
        assertEquals(imageQr.imagePath, state.qrCode!!.imagePath)
        assertEquals("", state.pendingDeletePath)
    }

    @Test
    fun `save with new image uploads first then updates and deletes the old file`() = runTest(testDispatcher) {
        load(imageQr)
        `when`(compressor.compress(pickedUri)).thenReturn(bytes)

        viewModel.onEvent(QrCodeEvent.OnImagePicked(pickedUri))
        viewModel.onEvent(QrCodeEvent.OnSaveQrCode)
        advanceUntilIdle()

        assertEquals(1, imageRepository.uploads.size)
        val (id, saved) = qrRepository.updates.single()
        assertEquals("qr1", id)
        assertEquals("https://storage/qr1/1.jpg", saved.imageUrl)
        assertEquals("qr_images/u1/qr1/1.jpg", saved.imagePath)
        assertEquals(listOf("qr_images/u1/qr1/old.jpg"), imageRepository.deleted)
        assertNull(viewModel.uiState.value.pendingImageUri)
        assertEquals(saved.imageUrl, viewModel.uiState.value.qrCode!!.imageUrl)
    }

    @Test
    fun `upload failure does not update the document`() = runTest(testDispatcher) {
        load(imageQr)
        `when`(compressor.compress(pickedUri)).thenReturn(bytes)
        imageRepository.uploadSucceeds = false
        val effects = mutableListOf<QrCodeEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        viewModel.onEvent(QrCodeEvent.OnImagePicked(pickedUri))
        viewModel.onEvent(QrCodeEvent.OnSaveQrCode)
        advanceUntilIdle()

        assertTrue(qrRepository.updates.isEmpty())
        assertTrue(imageRepository.deleted.isEmpty())
        assertEquals(imageQr.imageUrl, viewModel.uiState.value.qrCode!!.imageUrl)
        assertTrue(effects.any { it is QrCodeEffect.ShowToast })
        job.cancel()
    }

    @Test
    fun `saving IMAGE without any image is blocked`() = runTest(testDispatcher) {
        load(imageQr.copy(imageUrl = "", imagePath = ""))

        viewModel.onEvent(QrCodeEvent.OnSaveQrCode)
        advanceUntilIdle()

        assertTrue(qrRepository.updates.isEmpty())
    }

    @Test
    fun `failed document update removes the just uploaded file`() = runTest(testDispatcher) {
        load(imageQr)
        `when`(compressor.compress(pickedUri)).thenReturn(bytes)
        qrRepository.updateSucceeds = false

        viewModel.onEvent(QrCodeEvent.OnImagePicked(pickedUri))
        viewModel.onEvent(QrCodeEvent.OnSaveQrCode)
        advanceUntilIdle()

        assertEquals(listOf("qr_images/u1/qr1/1.jpg"), imageRepository.deleted)
        assertEquals(imageQr.imageUrl, viewModel.uiState.value.qrCode!!.imageUrl)
    }
}
