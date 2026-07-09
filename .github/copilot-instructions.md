# Copilot instructions for MySuperApp

## Build, test, and lint commands

### Android

- From the repository root on Windows, use the Gradle wrapper with `-p android` because the Gradle project lives under `android/` while `gradlew.bat` is at the repo root.
- Build the main Android app: `.\gradlew.bat -p android :app:assembleDevRelease`
- Build the QR manager app: `.\gradlew.bat -p android :app-qrcode:assembleDevRelease`
- Run Android unit tests for the main app: `.\gradlew.bat -p android :app:testDevReleaseUnitTest`
- Run a single Android test class: `.\gradlew.bat -p android :app:testDevReleaseUnitTest --tests "com.vfdeginformatica.mysuperapp.data.mapper.AccessLogMapperTest"`
- Run a single Android test method: `.\gradlew.bat -p android :app:testDevReleaseUnitTest --tests "com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.AccessLogMapViewModelTest.testToggleViewMode"`
- Lint the main Android app: `.\gradlew.bat -p android :app:lintDevRelease`
- Lint the QR manager app: `.\gradlew.bat -p android :app-qrcode:lintDevRelease`

### Firebase Functions / web backend

- Install Functions dependencies: `npm --prefix web\functions install`
- Run the Functions emulator: `npm --prefix web\functions run serve`
- Deploy Functions: `npm --prefix web\functions run deploy`
- Read Functions logs: `npm --prefix web\functions run logs`

## High-level architecture

- This is a split Android + Firebase web monorepo. Android code lives in `android/`; the hosted QR page and Firebase backend live in `web/`.
- `android/app` and `android/app-qrcode` are separate Android application modules. Both depend on the shared library module `android/feature-qrcode`.
- `android/feature-qrcode` contains most QR-specific code: Firebase-backed data sources and repositories, domain models and use cases, and reusable Compose screens such as login, QR detail, QR list, mural comments, and access-log map flows.
- `android/app` is the broader app shell. It adds the main app navigation and non-QR features such as home, financial screens, transactions, cards, and biometric authentication.
- `android/app-qrcode` is a dedicated QR manager shell. It reuses the shared QR feature module and adds its own nav graph, session flow, profile, notifications, and FCM service.
- The web side is not a bundled frontend app. `web/public/qr/index.html` is a static Firebase Hosting page with inline JavaScript that initializes Firebase directly in the browser.
- Firebase Hosting rewrites `/api/log-qr-scan` and `/api/add-mural-comment` to HTTPS Cloud Functions in `web/functions/index.js`.
- QR scan logs and mural comments are stored under Firestore subcollections on `qrcodes/{qrCodeId}`. Firestore-triggered Functions then send FCM notifications to the QR owner, and the Android shared module reads and updates the same QR code, access log, and comment data.

## Key conventions

- When changing QR-related behavior, check all three Android modules. Shared QR business logic usually belongs in `android/feature-qrcode`, while `android/app` and `android/app-qrcode` only provide app-specific shells, navigation, and DI wiring.
- Android code follows a layered structure across modules: `data/remote` and `data/local` for Firebase/device access, `domain` for models/repositories/use cases, and `presentation` for Compose UI and view models.
- Screen code follows a consistent pattern: `*Route.kt` handles navigation/composition, `*Screen.kt` renders UI, `*ViewModel.kt` owns behavior, and `contract/` packages hold `UiState`, `Event`, and `Effect` types. New screens should match that structure.
- Hilt wiring is app-local. Each Android application module has its own `di/DaoModule.kt`, `RepositoryModule.kt`, `UseCaseModule.kt`, and `UtilModule.kt`, even when they bind implementations from the shared QR feature module.
- Mapping between Firebase DTOs and domain models is explicit. Reuse the mapper functions in `data/mapper` and the existing `toDto`/`toModel` conversions instead of pushing DTO types into presentation code.
- Android flavors are `dev` and `prd`, and debug build variants are disabled in both application modules. Prefer flavor-aware release tasks such as `assembleDevRelease`, `testDevReleaseUnitTest`, and `lintDevRelease` instead of assuming plain `debug` tasks exist.
- `BuildConfig.IS_DEBUGGABLE` controls Firebase App Check setup. `dev` uses the debug App Check provider and `prd` uses Play Integrity. Preserve that split when touching application startup or comment/scan flows.
- Google Maps API keys are read from `android/local.properties` via `MAPS_API_KEY` and injected through manifest placeholders in both Android application modules. Do not hardcode Maps keys in source files.
- The QR web page is intentionally simple and mostly inline. If you change the hosted QR flow, expect behavior to be driven directly from `web/public/qr/index.html` plus the matching Cloud Functions rather than from a separate JS build system.
