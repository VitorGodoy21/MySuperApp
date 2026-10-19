package com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement
import com.vfdeginformatica.mysuperapp.domain.repository.BodyMeasurementRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.readFlow
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.writeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetBodyMeasurementsUseCase @Inject constructor(
    private val repository: BodyMeasurementRepository,
) {
    /** Newest first. */
    operator fun invoke(): Flow<Resource<List<BodyMeasurement>>> =
        readFlow("Erro ao carregar dados físicos") {
            repository.getMeasurements()?.sortedByDescending { it.measuredAt }
        }
}

class AddBodyMeasurementUseCase @Inject constructor(
    private val repository: BodyMeasurementRepository,
) {
    operator fun invoke(measurement: BodyMeasurement): Flow<Resource<String>> = flow {
        validate(measurement)?.let { emit(Resource.Error(it)); return@flow }
        writeFlow("Erro ao salvar dados físicos") { repository.addMeasurement(measurement) }
            .collect { emit(it) }
    }
}

class UpdateBodyMeasurementUseCase @Inject constructor(
    private val repository: BodyMeasurementRepository,
) {
    operator fun invoke(measurement: BodyMeasurement): Flow<Resource<Unit>> = flow {
        if (measurement.id.isBlank()) {
            emit(Resource.Error("Registro inválido")); return@flow
        }
        validate(measurement)?.let { emit(Resource.Error(it)); return@flow }
        writeFlow("Erro ao atualizar dados físicos") { repository.updateMeasurement(measurement) }
            .collect { emit(it) }
    }
}

class DeleteBodyMeasurementUseCase @Inject constructor(
    private val repository: BodyMeasurementRepository,
) {
    operator fun invoke(id: String): Flow<Resource<Unit>> = flow {
        if (id.isBlank()) {
            emit(Resource.Error("Registro inválido")); return@flow
        }
        writeFlow("Erro ao excluir dados físicos") { repository.deleteMeasurement(id) }
            .collect { emit(it) }
    }
}

internal fun validate(measurement: BodyMeasurement): String? = when {
    measurement.weightKg <= 0.0 -> "Peso deve ser maior que zero"
    measurement.heightCm <= 0.0 -> "Altura deve ser maior que zero"
    else -> null
}
