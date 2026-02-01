package com.vfdeginformatica.mysuperapp.di

import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.data.remote.datasource.AuthDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.UserRemoteDao
import com.vfdeginformatica.mysuperapp.data.remote.repository.UserRepository
import com.vfdeginformatica.mysuperapp.data.remote.repository.UserRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideUserRepository(
        userRemoteDao: UserRemoteDao,
        authDao: AuthDao,
        userSessionSecureStorage: UserSessionSecureStorage,
        useRemoteDao: UserRemoteDao
    ): UserRepository {
        return UserRepositoryImpl(userRemoteDao, authDao, userSessionSecureStorage, useRemoteDao)
    }
}