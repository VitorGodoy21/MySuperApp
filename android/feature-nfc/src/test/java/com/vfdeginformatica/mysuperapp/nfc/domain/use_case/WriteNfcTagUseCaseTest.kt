package com.vfdeginformatica.mysuperapp.nfc.domain.use_case

import android.nfc.Tag
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationError
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationResult
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcWriteContent
import com.vfdeginformatica.mysuperapp.nfc.domain.repository.NfcTagRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class WriteNfcTagUseCaseTest {

    private val tag: Tag = mock(Tag::class.java)

    @Test
    fun `invoke emits Loading then Success when repository write succeeds`() = runTest {
        var receivedContent: NfcWriteContent? = null
        val repository = object : NfcTagRepository {
            override suspend fun read(tag: Tag) = error("not used")
            override suspend fun write(tag: Tag, content: NfcWriteContent): NfcOperationResult<Unit> {
                receivedContent = content
                return NfcOperationResult.Success(Unit)
            }
            override suspend fun lock(tag: Tag) = error("not used")
        }
        val useCase = WriteNfcTagUseCase(repository)
        val content = NfcWriteContent.Url("https://baila.space/qr/?id=abc123&source=nfc")

        val emissions = useCase(tag, content).toList()

        assertTrue(emissions[0] is Resource.Loading)
        assertTrue(emissions[1] is Resource.Success)
        assertEquals(content, receivedContent)
    }

    @Test
    fun `invoke emits Loading then Success when writing custom text`() = runTest {
        var receivedContent: NfcWriteContent? = null
        val repository = object : NfcTagRepository {
            override suspend fun read(tag: Tag) = error("not used")
            override suspend fun write(tag: Tag, content: NfcWriteContent): NfcOperationResult<Unit> {
                receivedContent = content
                return NfcOperationResult.Success(Unit)
            }
            override suspend fun lock(tag: Tag) = error("not used")
        }
        val useCase = WriteNfcTagUseCase(repository)
        val content = NfcWriteContent.CustomText("valor personalizado")

        val emissions = useCase(tag, content).toList()

        assertTrue(emissions[0] is Resource.Loading)
        assertTrue(emissions[1] is Resource.Success)
        assertEquals(content, receivedContent)
    }

    @Test
    fun `invoke emits Loading then Error when tag has no room for the message`() = runTest {
        val repository = object : NfcTagRepository {
            override suspend fun read(tag: Tag) = error("not used")
            override suspend fun write(tag: Tag, content: NfcWriteContent) =
                NfcOperationResult.Failure(NfcOperationError.TagTooSmall(200, 144))
            override suspend fun lock(tag: Tag) = error("not used")
        }
        val useCase = WriteNfcTagUseCase(repository)

        val emissions = useCase(
            tag,
            NfcWriteContent.Url("https://baila.space/qr/?id=abc123&source=nfc")
        ).toList()

        assertTrue(emissions[0] is Resource.Loading)
        val error = emissions[1] as Resource.Error
        assertEquals(NfcOperationError.TagTooSmall(200, 144).message, error.message)
    }
}
