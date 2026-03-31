package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeAccessLogDto
import com.vfdeginformatica.mysuperapp.domain.model.AccessLog
import org.junit.Test
import java.util.Date

class AccessLogMapperTest {

    @Test
    fun testQrCodeAccessLogDtoToAccessLog_MapsAllFields() {
        // Arrange
        val testDate = Date()
        val dto = QrCodeAccessLogDto(
            id = "log_1",
            qrCodeId = "qr_001",
            timestamp = testDate,
            latitude = -23.6939,
            longitude = -46.565,
            city = "São Bernardo do Campo",
            country = "BR",
            userAgent = "Mozilla/5.0",
            language = "pt-BR",
            timeZone = "America/Sao_Paulo",
            platform = "Android",
            screenWidth = 1080,
            screenHeight = 1920,
            pageUrl = "https://baila.space/qr/",
            pagePath = "/qr/"
        )

        // Act
        val accessLog = dto.toAccessLog()

        // Assert
        assert(accessLog.id == "log_1")
        assert(accessLog.qrCodeId == "qr_001")
        assert(accessLog.timestamp == testDate)
        assert(accessLog.latitude == -23.6939)
        assert(accessLog.longitude == -46.565)
        assert(accessLog.city == "São Bernardo do Campo")
        assert(accessLog.country == "BR")
        assert(accessLog.userAgent == "Mozilla/5.0")
        assert(accessLog.language == "pt-BR")
        assert(accessLog.timeZone == "America/Sao_Paulo")
        assert(accessLog.platform == "Android")
        assert(accessLog.screenWidth == 1080)
        assert(accessLog.screenHeight == 1920)
    }

    @Test
    fun testAccessLogToAccessLocation_WithValidCoordinates() {
        // Arrange
        val testDate = Date()
        val accessLog = AccessLog(
            id = "log_1",
            qrCodeId = "qr_001",
            timestamp = testDate,
            latitude = -23.6939,
            longitude = -46.565,
            city = "São Bernardo do Campo",
            country = "BR",
            userAgent = "Mozilla/5.0"
        )

        // Act
        val location = accessLog.toAccessLocation()

        // Assert
        assert(location != null)
        assert(location!!.latitude == -23.6939)
        assert(location.longitude == -46.565)
        assert(location.city == "São Bernardo do Campo")
        assert(location.country == "BR")
        assert(location.accessCount == 1)
        assert(location.lastAccessTime == testDate)
    }

    @Test
    fun testAccessLogToAccessLocation_WithNullLatitude() {
        // Arrange
        val accessLog = AccessLog(
            id = "log_1",
            qrCodeId = "qr_001",
            timestamp = Date(),
            latitude = null,
            longitude = -46.565,
            city = "São Bernardo do Campo",
            country = "BR",
            userAgent = "Mozilla/5.0"
        )

        // Act
        val location = accessLog.toAccessLocation()

        // Assert
        assert(location == null)
    }

    @Test
    fun testGroupByCityStatistics_AggregatesCorrectly() {
        // Arrange
        val testDate1 = Date(System.currentTimeMillis() - 100000)
        val testDate2 = Date(System.currentTimeMillis() - 50000)
        val testDate3 = Date()

        val logs = listOf(
            AccessLog(
                id = "log_1",
                qrCodeId = "qr_001",
                timestamp = testDate1,
                latitude = -23.6939,
                longitude = -46.565,
                city = "São Bernardo do Campo",
                country = "BR",
                userAgent = "Mozilla/5.0"
            ),
            AccessLog(
                id = "log_2",
                qrCodeId = "qr_001",
                timestamp = testDate2,
                latitude = -23.6939,
                longitude = -46.565,
                city = "São Bernardo do Campo",
                country = "BR",
                userAgent = "Mozilla/5.0"
            ),
            AccessLog(
                id = "log_3",
                qrCodeId = "qr_001",
                timestamp = testDate3,
                latitude = -23.5505,
                longitude = -46.6333,
                city = "São Paulo",
                country = "BR",
                userAgent = "Mozilla/5.0"
            )
        )

        // Act
        val statistics = logs.groupByCityStatistics()

        // Assert
        assert(statistics.size == 2)

        val sbdoStats = statistics.find { it.city == "São Bernardo do Campo" }
        assert(sbdoStats != null)
        assert(sbdoStats!!.accessCount == 2)
        assert(sbdoStats.lastAccessTime == testDate2)

        val spStats = statistics.find { it.city == "São Paulo" }
        assert(spStats != null)
        assert(spStats!!.accessCount == 1)
        assert(spStats.lastAccessTime == testDate3)
    }

    @Test
    fun testGroupByCityStatistics_SortedByAccessCount() {
        // Arrange
        val logs = listOf(
            AccessLog("1", "qr_001", Date(), -23.6939, -46.565, "City A", "BR", "UA"),
            AccessLog("2", "qr_001", Date(), -23.6939, -46.565, "City A", "BR", "UA"),
            AccessLog("3", "qr_001", Date(), -23.6939, -46.565, "City A", "BR", "UA"),
            AccessLog("4", "qr_001", Date(), -23.5505, -46.6333, "City B", "BR", "UA"),
            AccessLog("5", "qr_001", Date(), -23.5505, -46.6333, "City B", "BR", "UA"),
            AccessLog("6", "qr_001", Date(), -22.9068, -43.1729, "City C", "BR", "UA")
        )

        // Act
        val statistics = logs.groupByCityStatistics()

        // Assert
        assert(statistics[0].city == "City A")
        assert(statistics[0].accessCount == 3)
        assert(statistics[1].city == "City B")
        assert(statistics[1].accessCount == 2)
        assert(statistics[2].city == "City C")
        assert(statistics[2].accessCount == 1)
    }

    @Test
    fun testGroupByCityStatistics_FiltersOutEmptyCities() {
        // Arrange
        val logs = listOf(
            AccessLog("1", "qr_001", Date(), -23.6939, -46.565, "São Paulo", "BR", "UA"),
            AccessLog("2", "qr_001", Date(), -23.6939, -46.565, "", "BR", "UA"), // Empty city
            AccessLog("3", "qr_001", Date(), -23.5505, -46.6333, "Rio de Janeiro", "BR", "UA")
        )

        // Act
        val statistics = logs.groupByCityStatistics()

        // Assert
        assert(statistics.size == 2)
        assert(statistics.all { it.city.isNotEmpty() })
    }
}

