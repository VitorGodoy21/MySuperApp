package com.vfdeginformatica.mysuperapp.di

import com.vfdeginformatica.mysuperapp.data.remote.repository.UserRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.IsLoggedUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideIsLoggedUseCase(
        userRepository: UserRepository
    ): IsLoggedUseCase {
        return IsLoggedUseCase(
            userRepository
        )
    }
}