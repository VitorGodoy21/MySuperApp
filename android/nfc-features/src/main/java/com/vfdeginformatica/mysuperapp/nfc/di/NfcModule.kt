package com.vfdeginformatica.mysuperapp.nfc.di

import com.vfdeginformatica.mysuperapp.nfc.data.NdefTagReaderWriter
import com.vfdeginformatica.mysuperapp.nfc.domain.repository.NfcTagRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NfcModule {

    @Binds
    @Singleton
    abstract fun bindNfcTagRepository(impl: NdefTagReaderWriter): NfcTagRepository
}
