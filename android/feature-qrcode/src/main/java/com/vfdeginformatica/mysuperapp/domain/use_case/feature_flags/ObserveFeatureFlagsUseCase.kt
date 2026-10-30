package com.vfdeginformatica.mysuperapp.domain.use_case.feature_flags

import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import com.vfdeginformatica.mysuperapp.domain.repository.FeatureFlagsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFeatureFlagsUseCase @Inject constructor(
    private val repository: FeatureFlagsRepository
) {
    operator fun invoke(): Flow<FeatureFlags> = repository.observe()
}
