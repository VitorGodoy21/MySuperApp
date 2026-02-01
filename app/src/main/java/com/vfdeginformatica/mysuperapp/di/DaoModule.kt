package com.vfdeginformatica.mysuperapp.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorageImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.AuthDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.AuthDaoImpl
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

}