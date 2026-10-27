package com.vfdeginformatica.mysuperapp.domain.use_case.qrcode

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import com.vfdeginformatica.mysuperapp.domain.model.MuralComment
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountQrCodesUseCaseTest {

    private fun repo(result: List<QrCodeDto>?) = object : QrCodeRepository {
        override suspend fun getQrCodes(): List<QrCodeDto>? = result
        override suspend fun getQrCode(id: String): QrCodeDto? = null
        override suspend fun createQrCode(qrCode: QrCode): QrCodeDto? = null
        override suspend fun updateQrCode(id: String, qrCode: QrCode): Boolean = false
        override suspend fun getComments(qrCodeId: String): List<MuralComment>? = null
        override suspend fun deleteComment(qrCodeId: String, commentId: String): Boolean = false
        override suspend fun addComment(qrCodeId: String, comment: MuralComment): Boolean = false
    }

    @Test
    fun `emits the list size`() = runTest {
        val result = CountQrCodesUseCase(repo(listOf(QrCodeDto(id = "a"), QrCodeDto(id = "b"))))().toList().last()

        assertTrue(result is Resource.Success)
        assertEquals(2, result.data)
    }

    @Test
    fun `null read is an error`() = runTest {
        val result = CountQrCodesUseCase(repo(null))().toList().last()

        assertTrue(result is Resource.Error)
    }
}
