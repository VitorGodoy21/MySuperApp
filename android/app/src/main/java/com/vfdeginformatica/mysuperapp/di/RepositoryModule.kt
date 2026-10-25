package com.vfdeginformatica.mysuperapp.di

import android.content.Context
import com.vfdeginformatica.mysuperapp.data.remote.datasource.DietDao
import com.vfdeginformatica.mysuperapp.data.remote.repository.DietRepositoryImpl
import com.vfdeginformatica.mysuperapp.domain.repository.DietRepository
import com.vfdeginformatica.mysuperapp.data.local.BiometricRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CardDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.TransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.BodyMeasurementDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.ExerciseDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.WorkoutDao
import com.vfdeginformatica.mysuperapp.data.remote.datasource.WorkoutSessionDao
import com.vfdeginformatica.mysuperapp.data.remote.repository.BodyMeasurementRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.repository.ExerciseRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.repository.WorkoutRepositoryImpl
import com.vfdeginformatica.mysuperapp.data.remote.repository.WorkoutSessionRepositoryImpl
import com.vfdeginformatica.mysuperapp.domain.repository.BodyMeasurementRepository
import com.vfdeginformatica.mysuperapp.domain.repository.ExerciseRepository
import com.vfdeginformatica.mysuperapp.domain.repository.WorkoutRepository
import com.vfdeginformatica.mysuperapp.domain.repository.WorkoutSessionRepository
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
    fun provideDietRepository(dao: DietDao): DietRepository = DietRepositoryImpl(dao)

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
    fun provideBodyMeasurementRepository(dao: BodyMeasurementDao): BodyMeasurementRepository {
        return BodyMeasurementRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideExerciseRepository(dao: ExerciseDao): ExerciseRepository {
        return ExerciseRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideWorkoutRepository(dao: WorkoutDao): WorkoutRepository {
        return WorkoutRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideWorkoutSessionRepository(dao: WorkoutSessionDao): WorkoutSessionRepository {
        return WorkoutSessionRepositoryImpl(dao)
    }

    @Provides
    @Singleton
    fun provideBiometricRepository(
        @ApplicationContext context: Context
    ): BiometricRepository {
        return BiometricRepositoryImpl(context)
    }
}
