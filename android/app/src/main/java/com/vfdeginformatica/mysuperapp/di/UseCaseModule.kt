package com.vfdeginformatica.mysuperapp.di

import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.domain.repository.BiometricRepository
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeAccessLogRepository
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.card.GetCardsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.NewTransactionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category.GetTransactionsCategoriesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.login.IsLoggedUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.login.LoginUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.login.LogoutUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.AddMuralCommentUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.DeleteAccessLogUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.DeleteMuralCommentUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetAccessLogsByCityUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetAccessLogsWithLocationsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetCityAccessStatisticsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetMuralCommentsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodeAccessStatisticsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.SaveQrCodeAccessLogUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UpdateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.AuthenticateWithBiometricUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.GetUserSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides
    @Singleton
    fun provideIsLoggedUseCase(
        userRepository: UserRepository
    ): IsLoggedUseCase {
        return IsLoggedUseCase(
            userRepository
        )
    }

    @Provides
    @Singleton
    fun provideLoginUseCase(
        userRepository: UserRepository
    ): LoginUseCase {
        return LoginUseCase(
            userRepository
        )
    }

    @Provides
    @Singleton
    fun provideLogoutUseCase(
        userRepository: UserRepository
    ): LogoutUseCase {
        return LogoutUseCase(
            userRepository
        )
    }

    @Provides
    @Singleton
    fun provideGetUserSessionUseCase(
        localUserRepository: UserSessionSecureStorage
    ): GetUserSessionUseCase {
        return GetUserSessionUseCase(
            localUserRepository
        )
    }

    @Provides
    @Singleton
    fun provideNewTransactionUseCase(
        transactionRepository: TransactionRepository
    ): NewTransactionUseCase {
        return NewTransactionUseCase(
            transactionRepository
        )
    }

    @Provides
    @Singleton
    fun provideGetCardsUseCase(
        repository: CardRepository
    ): GetCardsUseCase {
        return GetCardsUseCase(
            repository
        )
    }

    @Provides
    @Singleton
    fun provideGetTransactionsCategoriesUseCase(
        repository: CategoryTransactionRepository
    ): GetTransactionsCategoriesUseCase {
        return GetTransactionsCategoriesUseCase(
            repository
        )
    }

    @Provides
    @Singleton
    fun provideAuthenticateWithBiometricUseCase(
        biometricRepository: BiometricRepository
    ): AuthenticateWithBiometricUseCase {
        return AuthenticateWithBiometricUseCase(biometricRepository)
    }

    @Provides
    @Singleton
    fun provideGetQrCodesUseCase(
        qrCodeRepository: QrCodeRepository,
        qrCodeGenerator: QrCodeGenerator
    ): GetQrCodesUseCase {
        return GetQrCodesUseCase(qrCodeRepository, qrCodeGenerator)
    }

    @Provides
    @Singleton
    fun provideUpdateQrCodeUseCase(
        qrCodeRepository: QrCodeRepository
    ): UpdateQrCodeUseCase {
        return UpdateQrCodeUseCase(qrCodeRepository)
    }

    @Provides
    @Singleton
    fun provideSaveQrCodeAccessLogUseCase(
        qrCodeAccessLogRepository: QrCodeAccessLogRepository
    ): SaveQrCodeAccessLogUseCase {
        return SaveQrCodeAccessLogUseCase(qrCodeAccessLogRepository)
    }

    @Provides
    @Singleton
    fun provideGetQrCodeAccessStatisticsUseCase(
        qrCodeAccessLogRepository: QrCodeAccessLogRepository
    ): GetQrCodeAccessStatisticsUseCase {
        return GetQrCodeAccessStatisticsUseCase(qrCodeAccessLogRepository)
    }

    @Provides
    @Singleton
    fun provideGetAccessLogsWithLocationsUseCase(
        qrCodeAccessLogRepository: QrCodeAccessLogRepository
    ): GetAccessLogsWithLocationsUseCase {
        return GetAccessLogsWithLocationsUseCase(qrCodeAccessLogRepository)
    }

    @Provides
    @Singleton
    fun provideGetCityAccessStatisticsUseCase(
        qrCodeAccessLogRepository: QrCodeAccessLogRepository
    ): GetCityAccessStatisticsUseCase {
        return GetCityAccessStatisticsUseCase(qrCodeAccessLogRepository)
    }

    @Provides
    @Singleton
    fun provideGetAccessLogsByCityUseCase(
        qrCodeAccessLogRepository: QrCodeAccessLogRepository
    ): GetAccessLogsByCityUseCase {
        return GetAccessLogsByCityUseCase(qrCodeAccessLogRepository)
    }

    @Provides
    @Singleton
    fun provideDeleteAccessLogUseCase(
        qrCodeAccessLogRepository: QrCodeAccessLogRepository
    ): DeleteAccessLogUseCase {
        return DeleteAccessLogUseCase(qrCodeAccessLogRepository)
    }

    @Provides
    @Singleton
    fun provideGetMuralCommentsUseCase(
        qrCodeRepository: QrCodeRepository
    ): GetMuralCommentsUseCase {
        return GetMuralCommentsUseCase(qrCodeRepository)
    }

    @Provides
    @Singleton
    fun provideDeleteMuralCommentUseCase(
        qrCodeRepository: QrCodeRepository
    ): DeleteMuralCommentUseCase {
        return DeleteMuralCommentUseCase(qrCodeRepository)
    }

    @Provides
    @Singleton
    fun provideAddMuralCommentUseCase(
        qrCodeRepository: QrCodeRepository
    ): AddMuralCommentUseCase {
        return AddMuralCommentUseCase(qrCodeRepository)
    }

}