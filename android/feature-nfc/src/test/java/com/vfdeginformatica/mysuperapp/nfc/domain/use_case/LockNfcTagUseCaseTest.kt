package com.vfdeginformatica.mysuperapp.nfc.domain.use_case

import android.nfc.Tag
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationError
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcOperationResult
import com.vfdeginformatica.mysuperapp.nfc.domain.repository.NfcTagRepository
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcWriteContent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class LockNfcTagUseCaseTest {

    private val tag: Tag = mock(Tag::class.java)

    @Test
    fun `invoke emits Loading then Success when repository lock succeeds`() = runTest {
        val repository = object : NfcTagRepository {
            override suspend fun read(tag: Tag) = error("not used")
            override suspend fun write(tag: Tag, content: NfcWriteContent) = error("not used")
            override suspend fun lock(tag: Tag) = NfcOperationResult.Success(Unit)
        }
        val useCase = LockNfcTagUseCase(repository)

        val emissions = useCase(tag).toList()

        assertTrue(emissions[0] is Resource.Loading)
        assertTrue(emissions[1] is Resource.Success)
    }

    @Test
    fun `invoke emits Loading then Error when tag is already read-only`() = runTest {
        val repository = object : NfcTagRepository {
            override suspend fun read(tag: Tag) = error("not used")
            override suspend fun write(tag: Tag, content: NfcWriteContent) = error("not used")
            override suspend fun lock(tag: Tag) =
                NfcOperationResult.Failure(NfcOperationError.TagReadOnly)
        }
        val useCase = LockNfcTagUseCase(repository)

        val emissions = useCase(tag).toList()

        assertTrue(emissions[0] is Resource.Loading)
        val error = emissions[1] as Resource.Error
        assertEquals(NfcOperationError.TagReadOnly.message, error.message)
    }
}
