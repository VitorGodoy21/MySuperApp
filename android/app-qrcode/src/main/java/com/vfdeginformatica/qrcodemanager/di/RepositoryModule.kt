package com.vfdeginformatica.qrcodemanager.di

import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.data.remote.datasource.AuthDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrCodeAccessLogDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrCodeDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.UserRemoteDao
import com.vfdeginformatica.mysuperapp.data.remote.repository.QrCodeAccessLogRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.repository.QrCodeRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.repository.UserRepositoryImpl
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeAccessLogRepository
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
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
        userSessionSecureStorage: UserSessionSecureStorage
    ): UserRepository = UserRepositoryImpl(
        userDao = userRemoteDao,
        authDao = authDao,
        userSessionSecureStorage = userSessionSecureStorage,
        userRemoteDao = userRemoteDao
    )

    @Provides
    @Singleton
    fun provideQrCodeRepository(qrCodeDao: QrCodeDao): QrCodeRepository =
        QrCodeRepositoryImpl(qrCodeDao)

    @Provides
    @Singleton
    fun provideQrCodeAccessLogRepository(
        qrCodeAccessLogDao: QrCodeAccessLogDao
    ): QrCodeAccessLogRepository = QrCodeAccessLogRepositoryImpl(qrCodeAccessLogDao)
}

