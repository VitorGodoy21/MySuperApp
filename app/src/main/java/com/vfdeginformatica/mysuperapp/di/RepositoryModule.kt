package com.vfdeginformatica.mysuperapp.di

import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.data.remote.datasource.AuthDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.TransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.UserRemoteDao
import com.vfdeginformatica.mysuperapp.data.remote.repository.CardRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.repository.CategoryTransactionRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.repository.TransactionRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.repository.UserRepositoryImpl
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
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
        userSessionSecureStorage: UserSessionSecureStorage,
        useRemoteDao: UserRemoteDao
    ): UserRepository {
        return UserRepositoryImpl(userRemoteDao, authDao, userSessionSecureStorage, useRemoteDao)
    }

    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: TransactionDao
    ): TransactionRepository {
        return TransactionRepositoryImpl(transactionDao)
    }

    @Provides
    @Singleton
    fun provideCardRepository(
        cardDao: CardDao
    ): CardRepository {
        return CardRepositoryImpl(cardDao)
    }

    @Provides
    @Singleton
    fun provideCategoryTransactionRepository(
        categoryTransactionDao: CategoryTransactionDao
    ): CategoryTransactionRepository {
        return CategoryTransactionRepositoryImpl(categoryTransactionDao)
    }
}