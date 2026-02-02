package com.vfdeginformatica.mysuperapp.di

import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.data.remote.repository.UserRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.login.IsLoggedUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.login.LoginUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.login.LogoutUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.GetUserSessionUseCase
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

    @Provides
    @Singleton
    fun provideLoginUseCase(
        userRepository: UserRepository
    ): LoginUseCase {
        return LoginUseCase(
            userRepository
        )
    }

    @Provides
    @Singleton
    fun provideLogoutUseCase(
        userRepository: UserRepository
    ): LogoutUseCase {
        return LogoutUseCase(
            userRepository
        )
    }

    @Provides
    @Singleton
    fun provideGetUserSessionUseCase(
        localUserRepository: UserSessionSecureStorage
    ): GetUserSessionUseCase {
        return GetUserSessionUseCase(
            localUserRepository
        )
    }

}