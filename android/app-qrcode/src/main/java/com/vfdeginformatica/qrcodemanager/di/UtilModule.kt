package com.vfdeginformatica.qrcodemanager.di

import com.vfdeginformatica.mysuperapp.data.util.QrCodeGeneratorImpl
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {

    @Provides
    @Singleton
    fun provideQrCodeGenerator(): QrCodeGenerator = QrCodeGeneratorImpl()
}

