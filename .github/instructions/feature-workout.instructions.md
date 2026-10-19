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
  - `domain/model` — `BodyMeasurement` (computed `bmi`), `Exercise`, `MuscleGroup` (fixed enum with pt-BR `label`), `Workout`, `WorkoutItem`, `WeekDay`.
  - `domain/use_case/workout/{body_measurement,exercise,workout}` — use cases returning `Flow<Resource<T>>` (Loading → Success/Error) built on the `readFlow`/`writeFlow` helpers in `ResourceFlows.kt`.
  - `presentation/common` — `WorkoutFormatters` (dates, kg/cm, IMC, decimal/int input sanitizers) and `WeightChart` (Canvas line chart, no external lib).
  - `presentation/screen/<screen>` — `*Route.kt`, `*Screen.kt`, `*ViewModel.kt` and `contract/` (`UiState`, `Event`, `Effect`). Screens: `workout_hub`, `body_measurement_list`, `body_measurement_detail`, `exercise_list`, `exercise_detail`, `workout_list`, `workout_detail`.

## Dependencies and wiring

- Depends on `:feature-qrcode` only for `common.Resource` and `AppScaffold` (same caveat as `feature-financial`: a future `:core` module is the natural home for those).
- Hilt wiring is app-local: `android/app/di/DaoModule.kt` and `RepositoryModule.kt` provide the three DAOs/repositories. Use cases are `@Inject constructor` and need no module.
- Routes live in the shared `Screen` sealed class (`feature-qrcode/.../Screen.kt`): `workout_hub_screen`, `body_measurement_list_screen`, `body_measurement_detail_screen?measurementId={measurementId}`, `exercise_list_screen`, `exercise_detail_screen?exerciseId={exerciseId}`, `workout_list_screen`, `workout_detail_screen?workoutId={workoutId}`. Detail screens read the optional id from `SavedStateHandle` (blank/absent = create mode). The nav graph entries are in `android/app/.../MySuperApp.kt`.
- Entry points: Home card "Academia" (`HomeViewModel`, `passwordRequired = false`) → hub; drawer section "Academia" with "Dados físicos" / "Exercícios" / "Treinos" (`AppDrawerMenuRoute`, `navigateWithBiometric(..., passwordRequired = false)`). Unlike the financial section, no biometric prompt is required.

## Data model (Firestore, top-level collections, auth-only rules like the financial ones)

- `body_measurements/{id}`: `weightKg: number`, `heightCm: number`, `measuredAt: timestamp` (date of the weigh-in, stored at local start of day), `notes: string`, `createdAt` (server). One document per weigh-in → history. `GetBodyMeasurementsUseCase` returns newest first.
- `exercises/{id}`: `name` (required, non-empty), `muscleGroup` (enum name or `""`; unknown names map to `OTHER`, blank maps to `null`), `description`, `notes`, `createdAt`.
- `workouts/{id}`: `name` (required), `dayOfWeek` (`MONDAY`…`SUNDAY` or `null`), `items: list<map>` (ordered; each `exerciseId`, `order`, optional `sets`, `reps` (free text like `"10-12"`), `loadKg`, `restSeconds`, `notes`), `exerciseIds: list<string>`, `notes`, `createdAt`, `updatedAt`.
  - **Invariant:** `exerciseIds` is always derived from `items` in `Workout.toDto()` (never set by hand) so `WorkoutDao.getWorkoutsUsingExercise` can use `whereArrayContains("exerciseIds", id)`. `order` is re-indexed from list position on save and on read.
  - The same exercise may appear more than once in a workout; an item whose exercise no longer exists is rendered as "Exercício removido".
- Rules are in `web/firestore.rules` (`body_measurements`, `exercises`, `workouts`). Deploy with `firebase deploy --only firestore:rules` from `web\`.

## Business rules

- Body measurement validation (ViewModel): weight > 0 and ≤ 500 kg, height between 50 and 300 cm, date not in the future; decimal inputs accept comma or dot (`parseDecimal`). In create mode the height is prefilled from the latest record.
- Exercise: name required; a duplicate name (case-insensitive) only shows a warning. **Deleting an exercise that is referenced by any workout is blocked** (`DeleteExerciseUseCase` emits `Resource.Error` listing the workout names); the detail screen disables the delete button and explains which workouts use it.
- Workout: name required; items are optional (an empty workout can be saved and filled later); there is no limit on the number of workouts. Reordering is done with ▲/▼ buttons; item prescription (séries/repetições/carga/descanso/observação) is edited in a dialog and is optional.
- `GetWorkoutsUseCase` orders by `dayOfWeek` (unscheduled last) then name.

## How to use (user flow)

1. Home → "Academia" (or drawer → Academia). The hub shows the last weigh-in, exercise count and the list of workouts.
2. **Exercícios** → `+` to register exercises (name, muscle group chips, description, notes). Edit/delete by tapping an item.
3. **Treinos** → `+` → name (e.g. "Treino A"), optional week day, "Adicionar" to pick exercises from the catalog, reorder with the arrows, tap an item to set sets/reps/load/rest. Save. Repeat for B, C, … as needed.
4. **Dados físicos** → `+` to record weight/height/date/notes. The list shows deltas vs. the previous record, IMC and a weight chart; tap a row to edit, trash icon to delete.

## Tests

- Unit tests live in `src/test` (JUnit4 + `kotlinx-coroutines-test`, `StandardTestDispatcher` + `Dispatchers.setMain`, hand-written fakes in `domain/repository/Fake*Repository.kt`). Cover mappers, `DeleteExerciseUseCase`, UiState computations and ViewModel validation/save flows when changing behavior.
