package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.BodyMeasurementDto

interface BodyMeasurementDao {
    suspend fun getMeasurements(): List<BodyMeasurementDto>?
    suspend fun addMeasurement(measurement: BodyMeasurementDto): Resource<String>
    suspend fun updateMeasurement(measurement: BodyMeasurementDto): Resource<Unit>
    suspend fun deleteMeasurement(id: String): Resource<Unit>
}
