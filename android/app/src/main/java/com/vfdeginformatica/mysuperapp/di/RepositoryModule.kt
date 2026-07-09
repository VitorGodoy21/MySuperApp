package com.vfdeginformatica.mysuperapp.di

import android.content.Context
import com.vfdeginformatica.mysuperapp.data.local.BiometricRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.TransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.repository.CardRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.repository.CategoryTransactionRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.repository.TransactionRepositoryImpl
import com.vfdeginformatica.mysuperapp.domain.repository.BiometricRepository
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

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

    @Provides
    @Singleton
    fun provideBiometricRepository(
        @ApplicationContext context: Context
    ): BiometricRepository {
        return BiometricRepositoryImpl(context)
    }
}
