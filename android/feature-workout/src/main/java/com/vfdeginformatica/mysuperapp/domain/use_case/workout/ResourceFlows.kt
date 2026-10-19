package com.vfdeginformatica.mysuperapp.domain.use_case.workout

import com.vfdeginformatica.mysuperapp.common.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Wraps a nullable repository read as Loading → Success/Error. */
internal fun <T> readFlow(
    errorMessage: String,
    read: suspend () -> T?,
): Flow<Resource<T>> = flow {
    emit(Resource.Loading())
    val data = read()
    if (data != null) emit(Resource.Success(data)) else emit(Resource.Error(errorMessage))
}

/** Wraps a repository write returning [Resource] as Loading → Success/Error. */
internal fun <T> writeFlow(
    errorMessage: String,
    write: suspend () -> Resource<T>,
): Flow<Resource<T>> = flow {
    emit(Resource.Loading())
    when (val result = write()) {
        is Resource.Success -> emit(Resource.Success(result.data!!))
        is Resource.Error -> emit(Resource.Error(result.message ?: errorMessage))
        is Resource.Loading -> emit(Resource.Loading())
    }
}
