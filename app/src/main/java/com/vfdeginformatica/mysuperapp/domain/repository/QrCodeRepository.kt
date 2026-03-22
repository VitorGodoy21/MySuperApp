package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto

interface QrCodeRepository {
    suspend fun getQrCodes(): List<QrCodeDto>?
}
