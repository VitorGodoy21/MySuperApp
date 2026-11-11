---
applyTo: "android/feature-recipes/**"
---

# Recipes feature

- `:feature-recipes` is a flavorless Android library used only by `:app`, independent of Academia/Dieta.
  Keep the shared Kotlin root `com.vfdeginformatica.mysuperapp` and recipe-specific class names.
- Follow data/domain/presentation layering and Route/Screen/ViewModel/contract packages. Reuse
  `Resource`, `AppScaffold` and `ImageCompressor` from `:feature-qrcode`; app-local DI is in
  `android/app/.../di/DaoModule.kt` and `RepositoryModule.kt`.
- Routes in shared `Screen`: `recipe_list_screen`, `recipe_view_screen/{recipeId}`,
  `recipe_detail_screen?recipeId={recipeId}` (missing id = new recipe). Home and drawer open the list
  without biometrics. After creating, replace the form with details; after editing, return to details.
  List/details reload on resume.
- `Recipe` contains name, description, ordered `RecipeIngredient(name, quantity)` entries, ordered
  preparation steps, nullable prep/cook minutes, free-text yield, optional category/difficulty enums,
  notes, favorite and optional `imagePath`. Name/one complete ingredient/one nonblank step are required.
  `normalized()`/`validationError()` are shared by save and the form. Search/favorites are local and
  accent/case insensitive; list order is alphabetical.
- Firestore `recipes/{id}` carries immutable `userId`, plus server timestamps `createdAt`/`updatedAt`.
  Capture the expected uid at operation start, scope queries by it, verify ownership on point reads
  and reject writes after account changes. Client-reserved UUIDs make retrying creation reuse the id.
  Saving uses a transaction to preserve creation time and ownership; rules permit authenticated reads
  of absent ids for that transaction. No composite index or existing-data backfill is required.
- Editors/details invalidate themselves on logout/account change; list resets for the new account.
  Cancel pending operations and never render stale results. In-flight server writes cannot be undone
  by cancelling a coroutine, but must always target the captured account, never the new one.
- Photos are optional and PRIVATE: `recipe_images/{uid}/{recipeId}/{uuid}.jpg`, owner-only Storage
  read/write/delete, image MIME type and less than 5 MB. Photo Picker + shared JPEG compression
  (max 1600 px, 85%). Never reuse public QR image storage or persist token download URLs. Read through
  authenticated `getBytes`, render with Coil without persistent/shared image cache.
- Photo save order: upload replacement → save document → best-effort delete old file. On failed save,
  attempt cleanup of only the new upload; on failure keep draft for retry. Removing a photo saves a
  null path before cleanup. Delete document before best-effort photo cleanup. Firestore and Storage
  are not atomic; interrupted operations may leave orphan files. Do not promise complete rollback.
- The form has explicit Save, prevents duplicate saves, asks before discarding dirty drafts and
  supports ingredient/step reordering. No automatic save per keystroke.
- Independent flag `recipes` (missing = true): `FeatureFlags`, mapper, per-uid DataStore,
  `hasAnyEnabled`, Home/drawer visibility and `FeatureGate` for all three routes. Portal switch is
  in `web/public/my-super-app/index.html`; permitted flag keys are in Firestore rules.
- Use `.\gradlew.bat -p android :feature-recipes:testDebugUnitTest` and
  `.\gradlew.bat -p android :feature-recipes:lintDebug`; app build is `:app:assembleDevRelease`.
  Rule tests: `npm --prefix web\rules-tests ci`, then `npm --prefix web\rules-tests test` (Node 22+,
  Java 21+, isolated demo-project Firestore/Storage emulators, no production data).
- Before app use, publish matching rules and portal: from `web`, run
  `firebase deploy --only firestore:rules,storage,hosting --project <target-project>`.
  Publish to each intended Firebase environment explicitly; do not deploy automatically.
