package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto

interface QrCodeDao {
    suspend fun getQrCodes(): List<QrCodeDto>?
}
