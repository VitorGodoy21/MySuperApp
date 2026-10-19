package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement

interface BodyMeasurementRepository {
    suspend fun getMeasurements(): List<BodyMeasurement>?
    suspend fun addMeasurement(measurement: BodyMeasurement): Resource<String>
    suspend fun updateMeasurement(measurement: BodyMeasurement): Resource<Unit>
    suspend fun deleteMeasurement(id: String): Resource<Unit>
}
