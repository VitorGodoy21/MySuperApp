package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toDto
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.data.remote.datasource.BodyMeasurementDao
import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement
import com.vfdeginformatica.mysuperapp.domain.repository.BodyMeasurementRepository

class BodyMeasurementRepositoryImpl(
    private val dao: BodyMeasurementDao
) : BodyMeasurementRepository {

    override suspend fun getMeasurements(): List<BodyMeasurement>? =
        dao.getMeasurements()?.map { it.toModel() }

    override suspend fun addMeasurement(measurement: BodyMeasurement): Resource<String> =
        dao.addMeasurement(measurement.toDto())

    override suspend fun updateMeasurement(measurement: BodyMeasurement): Resource<Unit> =
        dao.updateMeasurement(measurement.toDto())

    override suspend fun deleteMeasurement(id: String): Resource<Unit> =
        dao.deleteMeasurement(id)
}
