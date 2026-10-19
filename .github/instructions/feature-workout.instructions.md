---
applyTo: "android/feature-workout/**"
---

# feature-workout module instructions

## Purpose and scope

- `android/feature-workout` is the gym/training feature library ("Academia") used only by the `:app` application module. It owns body-measurement history (peso/altura), the exercise catalog and user-assembled workouts (Treino A/B/C…, unlimited).
- It is an Android library without product flavors. Build and test it with `.\gradlew.bat -p android :feature-workout:compileDebugKotlin` and `.\gradlew.bat -p android :feature-workout:testDebugUnitTest`.

## Structure

- Kotlin packages stay under `com.vfdeginformatica.mysuperapp...` (matching `:app`) even though the Gradle namespace is `com.vfdeginformatica.feature.workout`. Do not change package names; app-side DI, imports and navigation depend on them.
- Layering (same as `feature-financial`):
  - `data/remote/datasource` — Firestore DAOs `BodyMeasurementDao`, `ExerciseDao`, `WorkoutDao` and their `*Impl` (constructor receives `FirebaseFirestore`). Writes use explicit field maps; reads use `toObject(Dto::class.java)`.
  - `data/remote/dto` — `BodyMeasurementDto`, `ExerciseDto`, `WorkoutDto` + nested `WorkoutItemDto`.
  - `data/remote/repository` — `*RepositoryImpl` wrapping DAOs; they map DTO↔domain so repositories and use cases only see domain models.
  - `data/mapper` — `BodyMeasurementMapper`, `ExerciseMapper`, `WorkoutMapper` (`toModel`/`toDto`).
  - `domain/model` — `BodyMeasurement` (computed `bmi`), `Exercise` (computed `videoId`), `MuscleGroup` (fixed enum with pt-BR `label`), `Workout`, `WorkoutItem`, `WeekDay`, `YouTubeVideo` (pure-Kotlin parser: `parseVideoId`, `canonicalUrl`, `embedUrl`).
  - `domain/use_case/workout/{body_measurement,exercise,workout}` — use cases returning `Flow<Resource<T>>` (Loading → Success/Error) built on the `readFlow`/`writeFlow` helpers in `ResourceFlows.kt`.
  - `presentation/common` — `WorkoutFormatters` (dates, kg/cm, IMC, decimal/int input sanitizers), `WeightChart` (Canvas line chart, no external lib) and `YouTubePlayer` / `YouTubePlayerDialog` (16:9 `WebView` running the official YouTube IFrame embed. The wrapper page is loaded with `YouTubeVideo.EMBED_ORIGIN` (the app's Firebase Hosting origin) as base URL and `origin=` param — YouTube rejects embeds without a valid `Referer` with "vídeo indisponível" 152-x/153, and `youtube.com` itself is not accepted. Non-embed links such as "Assistir no YouTube" are opened externally via `ACTION_VIEW`; the player's fullscreen button is honoured through `WebChromeClient.onShowCustomView` (handy for Shorts). Paused/resumed with the host lifecycle and destroyed on release — no YouTube SDK, no download).
  - `presentation/screen/<screen>` — `*Route.kt`, `*Screen.kt`, `*ViewModel.kt` and `contract/` (`UiState`, `Event`, `Effect`). Screens: `workout_hub` (view-mode list of workouts), `workout_view` (read-only workout + session log), `body_measurement_list`, `body_measurement_detail`, `exercise_list`, `exercise_detail`, `workout_list`, `workout_detail` (editing).

## Dependencies and wiring

- Depends on `:feature-qrcode` only for `common.Resource` and `AppScaffold` (same caveat as `feature-financial`: a future `:core` module is the natural home for those).
- Hilt wiring is app-local: `android/app/di/DaoModule.kt` and `RepositoryModule.kt` provide the three DAOs/repositories. Use cases are `@Inject constructor` and need no module.
- Routes live in the shared `Screen` sealed class (`feature-qrcode/.../Screen.kt`): `workout_hub_screen`, `workout_view_screen/{workoutId}` (required id), `body_measurement_list_screen`, `body_measurement_detail_screen?measurementId={measurementId}`, `exercise_list_screen`, `exercise_detail_screen?exerciseId={exerciseId}`, `workout_list_screen`, `workout_detail_screen?workoutId={workoutId}`. Detail screens read the optional id from `SavedStateHandle` (blank/absent = create mode). The nav graph entries are in `android/app/.../MySuperApp.kt`.
- Entry points: Home card "Academia" (`HomeViewModel`, `passwordRequired = false`) → hub = **view mode** (list of workouts → `workout_view`: no add/delete/reorder of exercises, but tapping an item edits its séries/reps/carga/descanso and saves right away; plus session logging); drawer section "Academia" with "Dados físicos" / "Exercícios" / "Treinos" (`AppDrawerMenuRoute`, `navigateWithBiometric(..., passwordRequired = false)`) = **edit mode** (`workout_list`/`workout_detail`). Unlike the financial section, no biometric prompt is required.

## Data model (Firestore, top-level collections, auth-only rules like the financial ones)

- `body_measurements/{id}`: `weightKg: number`, `heightCm: number`, `measuredAt: timestamp` (date of the weigh-in, stored at local start of day), `notes: string`, `createdAt` (server). One document per weigh-in → history. `GetBodyMeasurementsUseCase` returns newest first.
- `exercises/{id}`: `name` (required, non-empty), `muscleGroup` (enum name or `""`; unknown names map to `OTHER`, blank maps to `null`), `description`, `notes`, `videoUrl` (canonical `https://www.youtube.com/watch?v=<id>` or `""`; legacy documents without the field read as `""`), `createdAt`.
- `workouts/{id}`: `name` (required), `dayOfWeek` (`MONDAY`…`SUNDAY` or `null`), `items: list<map>` (ordered; each `exerciseId`, `order`, optional `sets`, `reps` (free text like `"10-12"`), `loadKg`, `restSeconds`, `notes`), `exerciseIds: list<string>`, `notes`, `createdAt`, `updatedAt`.
  - **Invariant:** `exerciseIds` is always derived from `items` in `Workout.toDto()` (never set by hand) so `WorkoutDao.getWorkoutsUsingExercise` can use `whereArrayContains("exerciseIds", id)`. `order` is re-indexed from list position on save and on read.
  - The same exercise may appear more than once in a workout; an item whose exercise no longer exists is rendered as "Exercício removido".
- `workout_sessions/{id}`: `workoutId: string` (required), `date: timestamp` (day of the gym visit, local start of day), `durationMinutes: int | null`, `calories: int | null`, `createdAt` (server). Add/delete only (no update). Queried with `whereEqualTo("workoutId", id)`; `GetWorkoutSessionsUseCase` sorts newest first in memory. Non-positive stats are read as `null`. Deleting a workout does **not** cascade to its sessions.
- Rules are in `web/firestore.rules` (`body_measurements`, `exercises`, `workouts`, `workout_sessions`). Deploy with `firebase deploy --only firestore:rules` from `web\`.
- To replicate the catalog and workouts between Firebase projects (dev `my-super-app-dev-d6819` → prd `baila-675c6`) use `npm --prefix web\functions run copy:workout-data -- --dry-run` / without `--dry-run` (`web/functions/scripts/copyWorkoutData.js`). It copies `exercises` then `workouts` preserving document IDs and overwrites existing ones; sessions and measurements are not copied.

## Business rules

- Body measurement validation (ViewModel): weight > 0 and ≤ 500 kg, height between 50 and 300 cm, date not in the future; decimal inputs accept comma or dot (`parseDecimal`). In create mode the height is prefilled from the latest record.
- Exercise: name required; a duplicate name (case-insensitive) only shows a warning. **Deleting an exercise that is referenced by any workout is blocked** (`DeleteExerciseUseCase` emits `Resource.Error` listing the workout names); the detail screen disables the delete button and explains which workouts use it.
- Exercise video: at most one YouTube link per exercise (`videoUrl`). The ViewModel accepts any shape `YouTubeVideo.parseVideoId` understands (`watch?v=`, `youtu.be/`, `shorts/`, `embed/`, `live/`, with or without scheme, extra params ignored); a non-blank value that does not parse shows `VIDEO_URL_INVALID_ERROR` and disables save. On save the URL is normalized with `YouTubeVideo.canonicalUrl`. The detail screen renders `YouTubePlayer` inline as soon as the link is valid; the workout screen shows a ▶ button on items whose exercise has a video (`WorkoutItemRow.videoId`) and opens `YouTubePlayerDialog` (`WorkoutDetailUiState.playingVideo`). Downloading YouTube videos is intentionally not supported (ToS).
- Workout: name required; items are optional (an empty workout can be saved and filled later); there is no limit on the number of workouts. Reordering is done with ▲/▼ buttons; item prescription (séries/repetições/carga/descanso/observação) is edited in a dialog and is optional.
- `GetWorkoutsUseCase` orders by `dayOfWeek` (unscheduled last) then name.
- Workout session (`WorkoutViewViewModel`, `AddWorkoutSessionUseCase.validateSession`): only the date is required and it cannot be in the future (`DATE_FUTURE_ERROR`); duration is in minutes, 1–600 (`DURATION_INVALID_ERROR`); calories 1–5000 (`CALORIES_INVALID_ERROR`); both optional (blank → `null`). Numeric inputs are sanitized to digits only (3/4 chars). `WorkoutViewUiState` reuses `WorkoutItemRow`/`WorkoutDetailUiState.prescriptionOf` for the read-only rows and exposes `lastSession`, `totalMinutes`, `totalCalories`.

## How to use (user flow)

1. Home → "Academia" shows "Meus treinos". Tap a workout to open the **view screen**: exercises with séries/repetições/carga/descanso as chips and ▶ for videos, plus the session history. Tap an exercise to adjust its prescription (`ItemDraftDialog` from `presentation/common/ItemDraftEditor.kt`, shared with `workout_detail`); the change is persisted immediately via `SaveWorkoutUseCase`. Adding/removing/reordering exercises is not possible here — use the drawer. FAB "Registrar treino" opens a dialog with date (today by default), minutes in the gym and calories; trash icon on a session row deletes it (with confirmation).
2. Drawer → Academia → **Exercícios** → `+` to register exercises (name, muscle group chips, description, notes, optional YouTube link — the video plays right below the field). Edit/delete by tapping an item.
3. Drawer → Academia → **Treinos** → `+` → name (e.g. "Treino A"), optional week day, "Adicionar" to pick exercises from the catalog, reorder with the arrows, tap an item to set sets/reps/load/rest, tap ▶ to watch the exercise video. Save. Repeat for B, C, … as needed.
4. Drawer → Academia → **Dados físicos** → `+` to record weight/height/date/notes. The list shows deltas vs. the previous record, IMC and a weight chart; tap a row to edit, trash icon to delete.

## Tests

- Unit tests live in `src/test` (JUnit4 + `kotlinx-coroutines-test`, `StandardTestDispatcher` + `Dispatchers.setMain`, hand-written fakes in `domain/repository/Fake*Repository.kt`). Cover mappers, `DeleteExerciseUseCase`, UiState computations and ViewModel validation/save flows when changing behavior.
