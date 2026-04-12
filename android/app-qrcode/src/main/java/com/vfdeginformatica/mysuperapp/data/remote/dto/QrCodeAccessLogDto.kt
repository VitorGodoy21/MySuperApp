package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

@IgnoreExtraProperties
data class QrCodeAccessLogDto(
    @DocumentId var id: String = "",
    var qrCodeId: String = "",
    var timestamp: Date = Date(),
    var latitude: Double? = null,
    var longitude: Double? = null,
    var city: String = "",
    var country: String = "",
    var userAgent: String = "",
    var language: String = "",
    var timeZone: String = "",
    var platform: String = "",
    var screenWidth: Int? = null,
    var screenHeight: Int? = null,
    var pageUrl: String = "",
    var pagePath: String = "",
    var referrer: String? = null,
    var scanId: String = "",
    var sessionId: String = "",
    var status: String = "completed",
    var method: String = ""
)

