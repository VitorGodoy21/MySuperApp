package com.vfdeginformatica.mysuperapp.di

import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.TransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.TransactionDaoImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    @Singleton
    fun provideTransactionDao(): TransactionDao {
        return TransactionDaoImpl(
            db = FirebaseFirestore.getInstance()
        )
    }

    @Provides
    @Singleton
    fun provideCardDao(): CardDao {
        return CardDaoImpl(
            db = FirebaseFirestore.getInstance()
        )
    }

    @Provides
    @Singleton
    fun provideCategoryTransactionDao(): CategoryTransactionDao {
        return CategoryTransactionDaoImpl(
            db = FirebaseFirestore.getInstance()
        )
    }
}