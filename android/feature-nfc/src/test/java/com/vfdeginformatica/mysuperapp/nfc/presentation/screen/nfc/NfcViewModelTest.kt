package com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc

import android.nfc.Tag
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import com.vfdeginformatica.mysuperapp.domain.model.MuralComment
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodesUseCase
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcContentType
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationError
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationResult
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcTagContent
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcWriteContent
import com.vfdeginformatica.mysuperapp.nfc.domain.repository.NfcTagRepository
import com.vfdeginformatica.mysuperapp.nfc.domain.use_case.LockNfcTagUseCase
import com.vfdeginformatica.mysuperapp.nfc.domain.use_case.ReadNfcTagUseCase
import com.vfdeginformatica.mysuperapp.nfc.domain.use_case.WriteNfcTagUseCase
import com.vfdeginformatica.mysuperapp.nfc.presentation.NfcTagBus
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcEvent
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcMode
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcWriteSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class NfcViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val tag: Tag = mock(Tag::class.java)

    private val qrCode = QrCode(
        id = "abc123",
        redirectUrl = "https://baila.space/qr/?id=abc123",
        staticUrl = "https://baila.space/qr/?id=abc123"
    )

    private var writeResult: NfcOperationResult<Unit> = NfcOperationResult.Success(Unit)
    private var lockResult: NfcOperationResult<Unit> = NfcOperationResult.Success(Unit)
    private var readResult: NfcOperationResult<NfcTagContent> = NfcOperationResult.Success(
        NfcTagContent(
            value = "https://baila.space/qr/?id=abc123",
            contentType = NfcContentType.URL,
            isWritable = true,
            maxSizeBytes = 144,
            usedSizeBytes = 40
        )
    )
    private var lastWrittenContent: NfcWriteContent? = null

    private val nfcTagRepository = object : NfcTagRepository {
        override suspend fun read(tag: Tag): NfcOperationResult<NfcTagContent> = readResult
        override suspend fun write(tag: Tag, content: NfcWriteContent): NfcOperationResult<Unit> {
            lastWrittenContent = content
            return writeResult
        }
        override suspend fun lock(tag: Tag): NfcOperationResult<Unit> = lockResult
    }

    private val qrCodeRepository = object : QrCodeRepository {
        override suspend fun getQrCodes() = listOf(
            QrCodeDto(id = qrCode.id, redirectUrl = qrCode.redirectUrl, staticUrl = qrCode.staticUrl)
        )
        override suspend fun createQrCode(qrCode: QrCode) = error("not used")
        override suspend fun updateQrCode(id: String, qrCode: QrCode) = error("not used")
        override suspend fun getComments(qrCodeId: String) = error("not used")
        override suspend fun deleteComment(qrCodeId: String, commentId: String) = error("not used")
        override suspend fun addComment(qrCodeId: String, comment: MuralComment) = error("not used")
    }

    private val qrCodeGenerator = object : QrCodeGenerator {
        override fun generate(content: String, size: Int) = null
    }

    private lateinit var viewModel: NfcViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = NfcViewModel(
            getQrCodesUseCase = GetQrCodesUseCase(qrCodeRepository, qrCodeGenerator),
            readNfcTagUseCase = ReadNfcTagUseCase(nfcTagRepository),
            writeNfcTagUseCase = WriteNfcTagUseCase(nfcTagRepository),
            lockNfcTagUseCase = LockNfcTagUseCase(nfcTagRepository),
            nfcTagBus = NfcTagBus()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `writing a tag appends source=nfc to the selected QrCode staticUrl`() = runTest(testDispatcher) {
        viewModel.onEvent(NfcEvent.OnStartWriting)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(NfcEvent.OnSelectQrCode(qrCode))
        viewModel.onEvent(NfcEvent.OnTagDiscovered(tag))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            NfcWriteContent.Url("https://baila.space/qr/?id=abc123&source=nfc"),
            lastWrittenContent
        )
        val state = viewModel.uiState.value
        assertTrue(state.showLockConfirmation)
        assertEquals(
            NfcWriteContent.Url("https://baila.space/qr/?id=abc123&source=nfc"),
            state.writeSuccessContent
        )
    }

    @Test
    fun `writing a tag with a custom text value writes it as-is`() = runTest(testDispatcher) {
        viewModel.onEvent(NfcEvent.OnStartWriting)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(NfcEvent.OnSelectWriteSource(NfcWriteSource.CUSTOM_TEXT))
        viewModel.onEvent(NfcEvent.OnCustomTextChanged("valor personalizado"))
        viewModel.onEvent(NfcEvent.OnConfirmCustomText)
        viewModel.onEvent(NfcEvent.OnTagDiscovered(tag))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(NfcWriteContent.CustomText("valor personalizado"), lastWrittenContent)
        val state = viewModel.uiState.value
        assertTrue(state.showLockConfirmation)
        assertEquals(NfcWriteContent.CustomText("valor personalizado"), state.writeSuccessContent)
    }

    @Test
    fun `writing a tag whose staticUrl already has a query string uses an ampersand`() = runTest(testDispatcher) {
        val qrCodeWithQuery = qrCode.copy(staticUrl = "https://baila.space/qr/?id=abc123&utm_source=print")

        viewModel.onEvent(NfcEvent.OnStartWriting)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(NfcEvent.OnSelectQrCode(qrCodeWithQuery))
        viewModel.onEvent(NfcEvent.OnTagDiscovered(tag))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            NfcWriteContent.Url("https://baila.space/qr/?id=abc123&utm_source=print&source=nfc"),
            lastWrittenContent
        )
    }

    @Test
    fun `confirming lock after a successful write waits for the same tag again`() = runTest(testDispatcher) {
        viewModel.onEvent(NfcEvent.OnStartWriting)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(NfcEvent.OnSelectQrCode(qrCode))
        viewModel.onEvent(NfcEvent.OnTagDiscovered(tag))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(NfcEvent.OnConfirmLock)

        var state = viewModel.uiState.value
        assertFalse(state.showLockConfirmation)
        assertTrue(state.pendingLock)
        assertTrue(state.isWaitingForTag)

        viewModel.onEvent(NfcEvent.OnTagDiscovered(tag))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.pendingLock)
        assertFalse(state.isWaitingForTag)
    }

    @Test
    fun `dismissing lock confirmation keeps the tag unlocked`() = runTest(testDispatcher) {
        viewModel.onEvent(NfcEvent.OnStartWriting)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(NfcEvent.OnSelectQrCode(qrCode))
        viewModel.onEvent(NfcEvent.OnTagDiscovered(tag))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(NfcEvent.OnDismissLockConfirmation)

        val state = viewModel.uiState.value
        assertFalse(state.showLockConfirmation)
        assertFalse(state.pendingLock)
    }

    @Test
    fun `starting a read resets the previous read content and enters reading mode`() = runTest(testDispatcher) {
        viewModel.onEvent(NfcEvent.OnStartReading)

        val state = viewModel.uiState.value
        assertEquals(NfcMode.READING, state.mode)
        assertTrue(state.isWaitingForTag)
        assertEquals(null, state.lastReadContent)
    }

    @Test
    fun `editing from the read screen switches to writing mode`() = runTest(testDispatcher) {
        viewModel.onEvent(NfcEvent.OnStartReading)
        viewModel.onEvent(NfcEvent.OnTagDiscovered(tag))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(NfcEvent.OnEditFromRead)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(NfcMode.WRITING, viewModel.uiState.value.mode)
    }

    @Test
    fun `locking permanently from the read screen waits for the same tag again`() = runTest(testDispatcher) {
        viewModel.onEvent(NfcEvent.OnStartReading)
        viewModel.onEvent(NfcEvent.OnTagDiscovered(tag))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(NfcEvent.OnLockPermanentlyFromRead)

        var state = viewModel.uiState.value
        assertTrue(state.pendingLock)
        assertTrue(state.isWaitingForTag)

        viewModel.onEvent(NfcEvent.OnTagDiscovered(tag))
        testDispatcher.scheduler.advanceUntilIdle()

        state = viewModel.uiState.value
        assertFalse(state.pendingLock)
        assertEquals(false, state.lastReadContent?.isWritable)
    }

    @Test
    fun `write failure surfaces the friendly error message and keeps waiting for a tag`() = runTest(testDispatcher) {
        writeResult = NfcOperationResult.Failure(NfcOperationError.TagTooSmall(200, 144))

        viewModel.onEvent(NfcEvent.OnStartWriting)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvent(NfcEvent.OnSelectQrCode(qrCode))
        viewModel.onEvent(NfcEvent.OnTagDiscovered(tag))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(NfcOperationError.TagTooSmall(200, 144).message, state.errorMessage)
        assertFalse(state.showLockConfirmation)
    }

    @Test
    fun `cancel resets the state while preserving nfc hardware status`() = runTest(testDispatcher) {
        viewModel.updateNfcHardwareStatus(isSupported = true, isEnabled = false)
        viewModel.onEvent(NfcEvent.OnStartReading)

        viewModel.onEvent(NfcEvent.OnCancel)

        val state = viewModel.uiState.value
        assertEquals(NfcMode.NONE, state.mode)
        assertFalse(state.isWaitingForTag)
        assertTrue(state.isNfcSupported)
        assertFalse(state.isNfcEnabled)
    }
}
