package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list

import android.net.Uri
import com.vfdeginformatica.mysuperapp.data.local.ImageCompressor
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.CreateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UpdateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UploadQrImageUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEvent
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class QrCodeListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val qrRepository = FakeQrCodeRepository(createdId = "new-id")
    private val imageRepository = FakeQrImageRepository()
    private val compressor: ImageCompressor = mock()
    private val pickedUri: Uri = mock()

    private lateinit var viewModel: QrCodeListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = QrCodeListViewModel(
            getQrCodesUseCase = GetQrCodesUseCase(qrRepository, NoopQrCodeGenerator),
            createQrCodeUseCase = CreateQrCodeUseCase(qrRepository, NoopQrCodeGenerator),
            updateQrCodeUseCase = UpdateQrCodeUseCase(qrRepository),
            uploadQrImageUseCase = UploadQrImageUseCase(imageRepository),
            imageCompressor = compressor
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `IMAGE creation chains create, upload and update`() = runTest(testDispatcher) {
        `when`(compressor.compress(pickedUri)).thenReturn(byteArrayOf(9))

        viewModel.onEvent(
            QrCodeListEvent.OnCreateQrCode(
                type = QrCodeType.IMAGE,
                identifier = "Foto",
                text = " Legenda ",
                imageUri = pickedUri
            )
        )
        advanceUntilIdle()

        assertEquals("Legenda", qrRepository.created.single().text)
        assertEquals("new-id", imageRepository.uploads.single().first)
        val (id, updated) = qrRepository.updates.single()
        assertEquals("new-id", id)
        assertEquals("https://storage/new-id/1.jpg", updated.imageUrl)
        assertEquals("qr_images/u1/new-id/1.jpg", updated.imagePath)
        assertFalse(viewModel.uiState.value.isCreating)
        assertFalse(viewModel.uiState.value.isUploadingImage)
    }

    @Test
    fun `upload failure keeps the created QR and warns the user`() = runTest(testDispatcher) {
        `when`(compressor.compress(pickedUri)).thenReturn(byteArrayOf(9))
        imageRepository.uploadSucceeds = false
        val effects = mutableListOf<QrCodeListEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }

        viewModel.onEvent(
            QrCodeListEvent.OnCreateQrCode(type = QrCodeType.IMAGE, identifier = "Foto", imageUri = pickedUri)
        )
        advanceUntilIdle()

        assertEquals(1, qrRepository.created.size)
        assertTrue(qrRepository.updates.isEmpty())
        assertTrue(
            effects.any {
                it is QrCodeListEffect.ShowToast && it.message == QrCodeListViewModel.IMAGE_UPLOAD_FAILED_MESSAGE
            }
        )
        assertFalse(viewModel.uiState.value.isCreating)
        job.cancel()
    }

    @Test
    fun `IMAGE creation without a picked image is rejected`() = runTest(testDispatcher) {
        viewModel.onEvent(QrCodeListEvent.OnCreateQrCode(type = QrCodeType.IMAGE, identifier = "Foto"))
        advanceUntilIdle()

        assertTrue(qrRepository.created.isEmpty())
    }

    @Test
    fun `TEXT creation ignores the image and does not upload`() = runTest(testDispatcher) {
        viewModel.onEvent(
            QrCodeListEvent.OnCreateQrCode(type = QrCodeType.TEXT, identifier = "T", text = "Olá", imageUri = pickedUri)
        )
        advanceUntilIdle()

        assertEquals("Olá", qrRepository.created.single().text)
        assertTrue(imageRepository.uploads.isEmpty())
        assertTrue(qrRepository.updates.isEmpty())
    }
}
