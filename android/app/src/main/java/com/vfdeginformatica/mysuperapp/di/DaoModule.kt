package com.vfdeginformatica.mysuperapp.di

import com.google.firebase.firestore.FirebaseFirestore
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.TransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.TransactionDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.BodyMeasurementDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.BodyMeasurementDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.ExerciseDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.ExerciseDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.WorkoutDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.WorkoutDaoImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.WorkoutSessionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.WorkoutSessionDaoImpl
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

    @Provides
    @Singleton
    fun provideBodyMeasurementDao(): BodyMeasurementDao {
        return BodyMeasurementDaoImpl(db = FirebaseFirestore.getInstance())
    }

    @Provides
    @Singleton
    fun provideExerciseDao(): ExerciseDao {
        return ExerciseDaoImpl(db = FirebaseFirestore.getInstance())
    }

    @Provides
    @Singleton
    fun provideWorkoutDao(): WorkoutDao {
        return WorkoutDaoImpl(db = FirebaseFirestore.getInstance())
    }

    @Provides
    @Singleton
    fun provideWorkoutSessionDao(): WorkoutSessionDao {
        return WorkoutSessionDaoImpl(db = FirebaseFirestore.getInstance())
    }
}