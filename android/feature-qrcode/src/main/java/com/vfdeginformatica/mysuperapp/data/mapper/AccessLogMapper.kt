package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeAccessLogDto
import com.vfdeginformatica.mysuperapp.domain.model.AccessLocation
import com.vfdeginformatica.mysuperapp.domain.model.AccessLog
import com.vfdeginformatica.mysuperapp.domain.model.CityAccessStatistics

/**
 * Mappers to convert between AccessLog DTO and domain models
 */

fun QrCodeAccessLogDto.toAccessLog(): AccessLog {
    return AccessLog(
        id = id,
        qrCodeId = qrCodeId,
        timestamp = timestamp,
        latitude = latitude,
        longitude = longitude,
        city = city,
        country = country,
        userAgent = userAgent,
        language = language,
        timeZone = timeZone,
        platform = platform,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        pageUrl = pageUrl,
        pagePath = pagePath
    )
}

fun AccessLog.toAccessLocation(): AccessLocation? {
    if (latitude == null || longitude == null) {
        return null
    }
    return AccessLocation(
        latitude = latitude,
        longitude = longitude,
        city = city,
        country = country,
        accessCount = 1,
        lastAccessTime = timestamp
    )
}

/**
 * Aggregate access logs by city to create city statistics
 */
fun List<AccessLog>.groupByCityStatistics(): List<CityAccessStatistics> {
    return this.groupBy { it.city to it.country }
        .mapNotNull { (cityCountry, logs) ->
            val (city, country) = cityCountry
            if (city.isEmpty()) null
            else {
                CityAccessStatistics(
                    city = city,
                    country = country,
                    accessCount = logs.size,
                    lastAccessTime = logs.maxByOrNull { it.timestamp }?.timestamp,
                    latitude = logs.firstOrNull { it.latitude != null }?.latitude,
                    longitude = logs.firstOrNull { it.longitude != null }?.longitude
                )
            }
        }
        .sortedByDescending { it.accessCount }
}

