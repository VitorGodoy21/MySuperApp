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
- Compile the NFC feature module: `.\gradlew.bat -p android :feature-nfc:compileDebugKotlin`
- Run unit tests for the NFC feature module: `.\gradlew.bat -p android :feature-nfc:testDebugUnitTest`
- Compile the financial feature module: `.\gradlew.bat -p android :feature-financial:compileDebugKotlin`
- Run unit tests for the financial feature module: `.\gradlew.bat -p android :feature-financial:testDebugUnitTest`
- Compile the workout (gym) feature module: `.\gradlew.bat -p android :feature-workout:compileDebugKotlin`
- Run unit tests for the workout feature module: `.\gradlew.bat -p android :feature-workout:testDebugUnitTest`

### Firebase Functions / web backend

- Install Functions dependencies: `npm --prefix web\functions install`
- Run the Functions emulator: `npm --prefix web\functions run serve`
- Deploy Functions: `npm --prefix web\functions run deploy`
- Read Functions logs: `npm --prefix web\functions run logs`
- Backfill `transactionType` (one-shot migration, Admin SDK credentials required): `npm --prefix web\functions run migrate:transaction-type -- --dry-run` to preview, then `npm --prefix web\functions run migrate:transaction-type` to apply.
- Copy gym catalog/workouts (`exercises`, `workouts`, IDs preserved) from the dev Firebase project to prd (ADC with access to both projects): `npm --prefix web\functions run copy:workout-data -- --dry-run` to preview, then `npm --prefix web\functions run copy:workout-data` to apply. Accepts `--source=`/`--target=`/`--collections=`.
- Deploy Firestore rules: `firebase deploy --only firestore:rules` (run from `web\`).

## High-level architecture

- This is a split Android + Firebase web monorepo. Android code lives in `android/`; the hosted QR page and Firebase backend live in `web/`.
- `android/app` and `android/app-qrcode` are separate Android application modules. Both depend on the shared library modules `android/feature-qrcode` and `android/feature-nfc`; `android/app` additionally depends on `android/feature-financial` and `android/feature-workout`.
- `android/feature-qrcode` contains most QR-specific code: Firebase-backed data sources and repositories, domain models and use cases, and reusable Compose screens such as login, QR detail, QR list, mural comments, and access-log map flows.
- `android/feature-nfc` contains the NFC tag reading/writing/locking feature: reading and writing NDEF records (URL or custom text), validating tag capacity, and locking a tag permanently (`Ndef.makeReadOnly()`). It has no product flavors, unlike `:app`/`:app-qrcode`.
- `android/feature-financial` contains the finance feature used only by `:app`: the financial home, new-transaction, card (`card_list`/`card_detail`/`new_card`), card-invoice (`card_invoice`, bank-statement view of one card's invoice for a given `yyyy-MM`) and transaction-category (`category_list`/`category_detail`) Compose screens, transaction/card/transaction-category domain repositories and use cases, and Firestore-backed DAOs/DTOs (`TransactionDao`, `CardDao`, `CategoryTransactionDao`). Both `transactions` and `transactions_category` documents carry `transactionType` = `EXPENSE` (despesa) | `INCOME` (receita); missing values are read as `EXPENSE`. It has no product flavors and depends on `:feature-qrcode` for the shared `common.Resource` wrapper and `AppScaffold` composable (a future `:core` module is the natural home for those shared pieces).
- `android/feature-workout` contains the gym feature ("Academia") used only by `:app`: body-measurement history (`body_measurements`, weight/height/date with a Canvas weight chart), an exercise catalog (`exercises`, fixed `MuscleGroup` enum, optional YouTube `videoUrl` played in-app via a `WebView` IFrame embed — `YouTubePlayer`/`YouTubePlayerDialog`, no download) and unlimited user-defined workouts (`workouts`, embedded ordered `items` with optional sets/reps/load/rest). `workouts.exerciseIds` is always derived from `items` so deleting an exercise still referenced by a workout is blocked (`DeleteExerciseUseCase`). Gym visits are logged per workout in `workout_sessions` (date, minutes, calories). Screens: `workout_hub` (Home → Academia: view-mode workout list), `workout_view` (workout view: prescription editable per item, no add/remove/reorder, plus session log), `body_measurement_list/detail`, `exercise_list/detail`, `workout_list/detail` (editing, reached from the drawer). Same flavorless setup and `:feature-qrcode` dependency as `feature-financial`; see `.github/instructions/feature-workout.instructions.md`.
- `android/app` is the broader app shell. It adds the main app navigation, home, and biometric authentication, and wires the financial feature module screens (`financial_screen`, `new_transaction_screen`, `card_invoice_screen/{cardId}/{invoiceMonth}`, card and category screens) into its nav graph and drawer ("Controle Financeiro" → Dashboard / Cartões / Categorias). QR Code and NFC are grouped under the "Smart Tags" topic: the home card uses the sentinel route `SmartTagsMenu.PICKER_ROUTE` (not a NavHost destination) so biometrics run on tap, then `SmartTagsPickerBottomSheet` lets the user pick QR Code or NFC without a second prompt; the drawer has an expandable "Smart Tags" group (QR Code / NFC subitems, each biometric-protected). Entry points to Finanças and Smart Tags from the home grid and the drawer are biometric-protected via `ResolveProtectedNavigationUseCase` (`HomeMenuItem.passwordRequired` / `AppDrawerMenuViewModel.navigateWithBiometric`); the prompt is shown on every entry, not once per session. The "Academia" home card and drawer section (Dados físicos / Exercícios / Treinos → `feature-workout` screens) are intentionally **not** biometric-protected (`passwordRequired = false`).
- `android/app-qrcode` is a dedicated QR manager shell. It reuses the shared QR feature module and adds its own nav graph, session flow, profile, notifications, and FCM service.
- The web side is not a bundled frontend app. `web/public/qr/index.html` is a static Firebase Hosting page with inline JavaScript that initializes Firebase directly in the browser.
- Firebase Hosting rewrites `/api/log-qr-scan` and `/api/add-mural-comment` to HTTPS Cloud Functions in `web/functions/index.js`.
- QR scan logs and mural comments are stored under Firestore subcollections on `qrcodes/{qrCodeId}`. Firestore-triggered Functions then send FCM notifications to the QR owner, and the Android shared module reads and updates the same QR code, access log, and comment data.

## Key conventions

- Android feature modules follow the naming convention `feature-<featureName>` (e.g. `feature-qrcode`, `feature-nfc`, `feature-financial`, `feature-workout`). New shared feature modules should be added under `android/feature-<name>` and registered in `android/settings.gradle.kts` with a matching `include(":feature-<name>")`.
- When changing QR-related behavior, check all three Android modules. Shared QR business logic usually belongs in `android/feature-qrcode`, while `android/app` and `android/app-qrcode` only provide app-specific shells, navigation, and DI wiring.
- Android code follows a layered structure across modules: `data/remote` and `data/local` for Firebase/device access, `domain` for models/repositories/use cases, and `presentation` for Compose UI and view models.
- Screen code follows a consistent pattern: `*Route.kt` handles navigation/composition, `*Screen.kt` renders UI, `*ViewModel.kt` owns behavior, and `contract/` packages hold `UiState`, `Event`, and `Effect` types. New screens should match that structure.
- Hilt wiring is app-local. Each Android application module has its own `di/DaoModule.kt`, `RepositoryModule.kt`, `UseCaseModule.kt`, and `UtilModule.kt`, even when they bind implementations from shared feature modules (e.g. `android/app/di` provides the financial DAOs/repositories implemented in `feature-financial`).
- Feature library modules keep the same Kotlin package root (`com.vfdeginformatica.mysuperapp...`) as `:app` even though their Gradle namespaces differ (e.g. `com.vfdeginformatica.feature.financial`). When moving code between modules, preserve packages so app-side imports, DI, and navigation stay unchanged.
- Mapping between Firebase DTOs and domain models is explicit. Reuse the mapper functions in `data/mapper` and the existing `toDto`/`toModel` conversions instead of pushing DTO types into presentation code.
- Android flavors are `dev` and `prd`, and debug build variants are disabled in both application modules. Prefer flavor-aware release tasks such as `assembleDevRelease`, `testDevReleaseUnitTest`, and `lintDevRelease` instead of assuming plain `debug` tasks exist.
- `BuildConfig.IS_DEBUGGABLE` controls Firebase App Check setup. `dev` uses the debug App Check provider and `prd` uses Play Integrity. Preserve that split when touching application startup or comment/scan flows.
- Google Maps API keys are read from `android/local.properties` via `MAPS_API_KEY` and injected through manifest placeholders in both Android application modules. Do not hardcode Maps keys in source files.
- The QR web page is intentionally simple and mostly inline. If you change the hosted QR flow, expect behavior to be driven directly from `web/public/qr/index.html` plus the matching Cloud Functions rather than from a separate JS build system.
