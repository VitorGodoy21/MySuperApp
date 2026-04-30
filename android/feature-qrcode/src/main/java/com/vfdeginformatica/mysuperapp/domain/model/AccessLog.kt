package com.vfdeginformatica.mysuperapp.domain.model

import java.util.Date

/**
 * Domain model representing a QR Code access log entry
 * Contains information about when and where a QR code was accessed
 */
data class AccessLog(
    val id: String,
    val qrCodeId: String,
    val timestamp: Date,
    val loggedAt: String = "",
    val latitude: Double?,
    val longitude: Double?,
    val city: String,
    val country: String,
    val userAgent: String,
    val language: String = "",
    val timeZone: String = "",
    val platform: String = "",
    val screenWidth: Int? = null,
    val screenHeight: Int? = null,
    val pageUrl: String = "",
    val pagePath: String = "",
    val referrer: String? = null,
    val scanId: String = "",
    val sessionId: String = "",
    val status: String = "",
    val method: String = "",
    val region: String = "",
    val utmSource: String? = null,
    val utmMedium: String? = null,
    val utmCampaign: String? = null
)

/**
 * Model for displaying access location information
 */
data class AccessLocation(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val country: String,
    val accessCount: Int = 1,
    val lastAccessTime: Date? = null
)

/**
 * Model for city-based access statistics
 */
data class CityAccessStatistics(
    val city: String,
    val country: String,
    val accessCount: Int,
    val lastAccessTime: Date? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

