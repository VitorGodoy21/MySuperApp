package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement

open class FakeBodyMeasurementRepository(
    var measurements: List<BodyMeasurement> = emptyList(),
) : BodyMeasurementRepository {
    val added = mutableListOf<BodyMeasurement>()
    val updated = mutableListOf<BodyMeasurement>()
    val deletedIds = mutableListOf<String>()
    var writeResult: Resource<Unit> = Resource.Success(Unit)

    override suspend fun getMeasurements(): List<BodyMeasurement>? = measurements

    override suspend fun addMeasurement(measurement: BodyMeasurement): Resource<String> {
        added += measurement
        return when (val result = writeResult) {
            is Resource.Error -> Resource.Error(result.message ?: "error")
            else -> Resource.Success("new-id")
        }
    }

    override suspend fun updateMeasurement(measurement: BodyMeasurement): Resource<Unit> {
        updated += measurement
        return writeResult
    }

    override suspend fun deleteMeasurement(id: String): Resource<Unit> {
        deletedIds += id
        return writeResult
    }
}
