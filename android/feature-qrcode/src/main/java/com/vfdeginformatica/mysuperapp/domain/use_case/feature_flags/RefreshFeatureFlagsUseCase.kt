package com.vfdeginformatica.mysuperapp.domain.use_case.feature_flags

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.FeatureFlags
import com.vfdeginformatica.mysuperapp.domain.repository.FeatureFlagsRepository
import javax.inject.Inject

class RefreshFeatureFlagsUseCase @Inject constructor(
    private val repository: FeatureFlagsRepository
) {
    suspend operator fun invoke(): Resource<FeatureFlags> = repository.refresh()
}
