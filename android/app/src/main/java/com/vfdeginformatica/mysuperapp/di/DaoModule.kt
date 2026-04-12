package com.vfdeginformatica.mysuperapp.di

import android.content.Context
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorageImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.AuthDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.AuthDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrCodeAccessLogDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrCodeAccessLogDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrCodeDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.QrCodeDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.TransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.TransactionDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.UserRemoteDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.UserRemoteDaoImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    @Singleton
    fun provideAuthDao(): AuthDao {
        return AuthDaoImpl(
            auth = FirebaseAuth.getInstance()
        )
    }

    @Provides
    @Singleton
    fun provideUserDao(): UserRemoteDao {
        return UserRemoteDaoImpl(
            db = FirebaseFirestore.getInstance()
        )
    }

    @Provides
    @Singleton
    fun provideUserSessionSecureStorage(
        @ApplicationContext context: Context
    ): UserSessionSecureStorage = UserSessionSecureStorageImpl(context)

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

    @Provides
    @Singleton
    fun provideQrcodeTransactionDao(): QrCodeDao {
        return QrCodeDaoImpl(
            db = FirebaseFirestore.getInstance(),
            appCheck = FirebaseAppCheck.getInstance(),
            auth = FirebaseAuth.getInstance()
        )
    }

    @Provides
    @Singleton
    fun provideQrCodeAccessLogDao(): QrCodeAccessLogDao {
        return QrCodeAccessLogDaoImpl(
            db = FirebaseFirestore.getInstance()
        )
    }

}