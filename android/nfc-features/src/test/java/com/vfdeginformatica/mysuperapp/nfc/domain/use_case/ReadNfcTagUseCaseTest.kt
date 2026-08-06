package com.vfdeginformatica.mysuperapp.nfc.domain.use_case

import android.nfc.Tag
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcContentType
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationError
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationResult
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcTagContent
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcWriteContent
import com.vfdeginformatica.mysuperapp.nfc.domain.repository.NfcTagRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class ReadNfcTagUseCaseTest {

    private val tag: Tag = mock(Tag::class.java)

    @Test
    fun `invoke emits Loading then Success when repository read succeeds`() = runTest {
        val content = NfcTagContent(
            value = "https://baila.space/qr/?id=abc123&source=nfc",
            contentType = NfcContentType.URL,
            isWritable = true,
            maxSizeBytes = 144,
            usedSizeBytes = 48
        )
        val repository = object : NfcTagRepository {
            override suspend fun read(tag: Tag) = NfcOperationResult.Success(content)
            override suspend fun write(tag: Tag, content: NfcWriteContent) = error("not used")
            override suspend fun lock(tag: Tag) = error("not used")
        }
        val useCase = ReadNfcTagUseCase(repository)

        val emissions = useCase(tag).toList()

        assertTrue(emissions[0] is Resource.Loading)
        val success = emissions[1] as Resource.Success
        assertEquals(content, success.data)
    }

    @Test
    fun `invoke emits Loading then Error with friendly message when repository read fails`() = runTest {
        val repository = object : NfcTagRepository {
            override suspend fun read(tag: Tag) = NfcOperationResult.Failure(NfcOperationError.TagLost)
            override suspend fun write(tag: Tag, content: NfcWriteContent) = error("not used")
            override suspend fun lock(tag: Tag) = error("not used")
        }
        val useCase = ReadNfcTagUseCase(repository)

        val emissions = useCase(tag).toList()

        assertTrue(emissions[0] is Resource.Loading)
        val error = emissions[1] as Resource.Error
        assertEquals(NfcOperationError.TagLost.message, error.message)
    }
}
