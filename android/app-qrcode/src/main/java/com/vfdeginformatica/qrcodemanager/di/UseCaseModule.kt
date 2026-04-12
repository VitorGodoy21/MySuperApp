package com.vfdeginformatica.qrcodemanager.di

import com.vfdeginformatica.mysuperapp.data.local.datasource.UserSessionSecureStorage
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeAccessLogRepository
import com.vfdeginformatica.mysuperapp.domain.repository.QrCodeRepository
import com.vfdeginformatica.mysuperapp.domain.repository.UserRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.login.IsLoggedUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.login.LoginUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.login.LogoutUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.AddMuralCommentUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.DeleteMuralCommentUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetAccessLogsByCityUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetAccessLogsWithLocationsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetCityAccessStatisticsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetMuralCommentsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodeAccessStatisticsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.SaveQrCodeAccessLogUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UpdateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.GetUserSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.validator.ValidateEmail
import com.vfdeginformatica.mysuperapp.domain.use_case.validator.ValidatePassword
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // ── Auth / Login ─────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideIsLoggedUseCase(r: UserRepository) = IsLoggedUseCase(r)

    @Provides
    @Singleton
    fun provideLoginUseCase(r: UserRepository) = LoginUseCase(r)

    @Provides
    @Singleton
    fun provideLogoutUseCase(r: UserRepository) = LogoutUseCase(r)

    @Provides
    @Singleton
    fun provideGetUserSessionUseCase(storage: UserSessionSecureStorage) =
        GetUserSessionUseCase(storage)

    @Provides
    @Singleton
    fun provideValidateEmail() = ValidateEmail()

    @Provides
    @Singleton
    fun provideValidatePassword() = ValidatePassword()

    // ── QR Code ───────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideGetQrCodesUseCase(r: QrCodeRepository, g: QrCodeGenerator) =
        GetQrCodesUseCase(r, g)

    @Provides
    @Singleton
    fun provideUpdateQrCodeUseCase(r: QrCodeRepository) = UpdateQrCodeUseCase(r)

    @Provides
    @Singleton
    fun provideSaveQrCodeAccessLogUseCase(r: QrCodeAccessLogRepository) =
        SaveQrCodeAccessLogUseCase(r)

    @Provides
    @Singleton
    fun provideGetQrCodeAccessStatisticsUseCase(r: QrCodeAccessLogRepository) =
        GetQrCodeAccessStatisticsUseCase(r)

    @Provides
    @Singleton
    fun provideGetAccessLogsWithLocationsUseCase(r: QrCodeAccessLogRepository) =
        GetAccessLogsWithLocationsUseCase(r)

    @Provides
    @Singleton
    fun provideGetCityAccessStatisticsUseCase(r: QrCodeAccessLogRepository) =
        GetCityAccessStatisticsUseCase(r)

    @Provides
    @Singleton
    fun provideGetAccessLogsByCityUseCase(r: QrCodeAccessLogRepository) =
        GetAccessLogsByCityUseCase(r)

    @Provides
    @Singleton
    fun provideGetMuralCommentsUseCase(r: QrCodeRepository) = GetMuralCommentsUseCase(r)

    @Provides
    @Singleton
    fun provideDeleteMuralCommentUseCase(r: QrCodeRepository) = DeleteMuralCommentUseCase(r)

    @Provides
    @Singleton
    fun provideAddMuralCommentUseCase(r: QrCodeRepository) = AddMuralCommentUseCase(r)
}

