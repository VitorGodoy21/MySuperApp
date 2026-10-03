---
applyTo: "android/feature-financial/**"
---

# feature-financial module instructions

## Purpose and scope

- `android/feature-financial` is the finance feature library used only by the `:app` application module. It owns the financial home screen, the new-transaction flow, and all transaction/card/transaction-category data access.
- It is an Android library without product flavors. Build and test it with `.\gradlew.bat -p android :feature-financial:compileDebugKotlin` and `.\gradlew.bat -p android :feature-financial:testDebugUnitTest`.

## Structure

- Kotlin packages stay under `com.vfdeginformatica.mysuperapp...` (matching `:app`) even though the Gradle namespace is `com.vfdeginformatica.feature.financial`. Do not change package names when adding or moving files; app-side DI, imports, and navigation depend on them.
- Follow the repository-wide layering inside this module:
  - `data/remote/datasource` — Firestore DAOs (`TransactionDao`, `CardDao`, `CategoryTransactionDao`) and their `*Impl` classes receiving `FirebaseFirestore` via constructor.
  - `data/remote/dto` — Firestore DTOs (`TransactionDto`, `CardDto`, `CategoryTransactionDto`).
  - `data/remote/repository` — repository implementations wrapping DAOs.
  - `data/mapper` — explicit DTO↔domain conversions (`TransactionMapper`); never expose DTOs to presentation code.
  - `domain/model`, `domain/repository`, `domain/use_case/financial/...` — models, repository interfaces, and use cases returning `Flow<Resource<T>>`.
  - `presentation/screen/<screen>` — `*Route.kt` (navigation/composition), `*Screen.kt` (UI), `*ViewModel.kt` (behavior), and `contract/` with `UiState`, `Event`, `Effect` types.

## Dependencies and wiring

- The module depends on `:feature-qrcode` only for shared infrastructure: the `common.Resource` wrapper and the `AppScaffold` composable. Do not add QR business logic dependencies; if more shared pieces are needed, prefer extracting a `:core` module.
- Hilt wiring is app-local: `android/app/di/DaoModule.kt` and `RepositoryModule.kt` provide the DAOs and repositories implemented here. When adding a new DAO/repository, add the corresponding `@Provides` bindings in `android/app/di`, not in this module.
- Navigation routes for this module's screens (`financial_screen`, `new_transaction_screen`, `new_card_screen`, `card_list_screen`, `card_detail_screen/{cardId}`, `category_list_screen`, `category_detail_screen?categoryId={categoryId}`) are declared in the shared `Screen` sealed class and composed into the nav graph in `android/app` (`MySuperApp.kt`). New screens need a route there plus a `composable(...)` entry in the app nav graph.

## Domain model (validated against the app flow)

- A transaction is either **INCOME (Receita)** or **EXPENSE (Despesa)** (`TransactionType`), chosen via chips on the new-transaction screen.
  - **INCOME**: title, amount, optional description, the destination card, and an optional "Receita recorrente" switch (e.g. salary) used by the projection tab. All cards are selectable (credit-only included); `allowsDeposit` no longer restricts the list. A card is required and no payment method is chosen.
  - **EXPENSE**: title, amount, optional description, a required card, a payment method (`DEBIT`/`CREDIT`) constrained by the card's `paymentMethods`, and multi-select categories. When the method is `CREDIT`, the user picks either a number of installments (1–12 or 24) or "Compra Recorrente" (`isRecurring = true`). When the method is `DEBIT`, a "Compra recorrente" switch is available instead. Switching card or payment method resets `isRecurring`. The payment-method section is rendered only for EXPENSE — INCOME picks just the destination card. Category chips are shown for both types, filtered to categories whose `transactionType` matches the selected type.
- `installments` is persisted only for non-recurring CREDIT expenses; otherwise it is `null`.
- Amount fields (new transaction and the edit dialog) use a cents-based currency mask: UI state holds only digits representing centavos (e.g. `"12480"` = R$ 124,80), rendered by `presentation/common/CurrencyVisualTransformation`. Convert with `CurrencyVisualTransformation.centsToDouble` before persisting — Firestore stores plain `Double` reais.
- `CardDto` (`cards` collection): `bank`, `finalCode`, `paymentMethods` (list of `DEBIT`/`CREDIT`), `invoiceDueDate`, `allowsDeposit`, `closingDay` (invoice closing day; `0` means "no closing day configured"). Cards can be created in-app via the `new_card` screen (reached from the card dropdown on the new-transaction screen): bank + payment-method chips, plus `finalCode`/`closingDay`/`invoiceDueDate` when CREDIT; `allowsDeposit` is always saved as `true`. Optional "Saldo inicial" and "Fatura atual" values are persisted as regular transactions (INCOME, and EXPENSE/CREDIT with computed `invoiceMonth`, respectively) so they show up in the dashboard/projection and stay editable through the edit dialog.
- Card management screens: the drawer's "Controle Financeiro" entry expands into "Dashboard" (FinancialScreen), "Cartões" (`card_list` screen) and "Categorias" (`category_list` screen). The card list navigates to the `card_detail` screen (`card_detail_screen/{cardId}` route, cardId read via `SavedStateHandle`), which edits `bank`/`paymentMethods`/`finalCode`/`closingDay`/`invoiceDueDate` through `UpdateCardUseCase` and has an "Adicionar saldo" section that creates an INCOME transaction ("Ajuste de saldo — {bank}") via `NewTransactionUseCase` instead of storing balance on the card doc.
- `transactions` documents carry: `title`, `description`, `transactionType` (**required**: `EXPENSE` | `INCOME`), `paymentMethod`, `amount`, `isRecurring`, `transactionDate`, `category` (list of category ids), `status`, `cardId`, `invoiceMonth`, `installmentGroupId`, `installments`, `installmentNumber`. `TransactionMapper.toModel()` treats a missing/unknown `transactionType` as `EXPENSE`.

### Transaction categories

- Collection `transactions_category/{categoryId}`: `title` (string, required, non-empty), `description` (string, optional, default `""`), `transactionType` (string, required: `EXPENSE` = despesa, `INCOME` = receita). A category belongs to exactly one type.
- Legacy documents created before the field existed are treated as `EXPENSE` by the client (`CategoryTransactionDto.transactionType` defaults to `"EXPENSE"`, and `TransactionCategoryMapper.toTransactionType()` falls back to `EXPENSE` for null/unknown values). Run the backfill script (see "Data") once so the field is physically present.
- Domain model is `domain/model/TransactionCategory` (`id`, `title`, `description`, `type: TransactionType`), converted via `data/mapper/TransactionCategoryMapper.kt` (`toModel()`/`toDto()`). New screens use the domain model; the pre-existing `NewTransactionUiState`/`FinancialUiState` still hold `CategoryTransactionDto` and read `transactionType` from it.
- Management screens:
  - `category_list` (`category_list_screen` route, drawer item "Categorias"): lists categories grouped under "Despesas"/"Receitas" headers, FAB "Nova categoria". Reloads on `LaunchedEffect(Unit)` like `card_list`.
  - `category_detail` (`category_detail_screen?categoryId={categoryId}` route; the `categoryId` nav arg is nullable — absent means "create" mode, present means "edit" mode; read via `SavedStateHandle`, key `CategoryDetailViewModel.CATEGORY_ID_ARG`): type chips (Despesa/Receita), title (required, max 40 chars), description (optional, max 120 chars), Save button, and in edit mode an "Excluir categoria" button guarded by an `AlertDialog`.
- Use cases (`domain/use_case/financial/transaction_category`): `GetTransactionsCategoriesUseCase`, `AddTransactionCategoryUseCase`, `UpdateTransactionCategoryUseCase`, `DeleteTransactionCategoryUseCase`.
- **Deleting a category** (`DeleteTransactionCategoryUseCase`) must never leave orphan ids in `transactions.category`. It first calls `TransactionRepository.removeCategoryFromTransactions(categoryId)` — `whereArrayContains("category", id)` + `WriteBatch` updates with `FieldValue.arrayRemove(id)` in chunks of 500 — and only if that succeeds calls `CategoryTransactionRepository.deleteCategory(id)`. On cleanup failure the category is kept and the user can retry.
- Type-aware category pickers: the new-transaction screen shows categories for **both** INCOME and EXPENSE, filtered by the selected type (`NewTransactionUiState.categoriesForType`); switching type drops selected categories that don't belong to the new type. The dashboard's category filter lists `FinancialUiState.categoriesForFilter` (all categories when the type filter is ALL, otherwise only that type) and clears `categoryFilterId` when it becomes incompatible.

### Credit-card invoice logic

- On save, `NewTransactionUseCase` computes `invoiceMonth` (format `yyyy-MM`) for CREDIT expenses from the card's `closingDay`: purchases on or after the closing day go to next month's invoice; before it, to the current month's. `closingDay == 0` keeps the purchase in the current month.
- Non-recurring CREDIT purchases with N > 1 installments are split into **N transaction documents** (amount/N with rounding on the last installment), one per sequential `invoiceMonth`, sharing an `installmentGroupId` (UUID) and carrying `installmentNumber` (1..N). They are written atomically with a Firestore `WriteBatch` (`TransactionDao.addTransactions`).
- Recurring transactions (`isRecurring == true`) exist as a single document and are projected into every month by the projection tab.

### Financial screen

- The financial home screen (`FinancialScreen`) has two tabs (`FinancialTab`):
  - **Mês**: the month-based dashboard — month navigation, period balance (income − expense), income/expense totals, and the transaction list filtered client-side by type, category, and card. A discreet "Só recorrentes" chip (`recurringExpensesOnly`) restricts the list to recurring expenses (forcing the type filter to EXPENSE) and shows the month's recurring-expense total (`monthRecurringExpenseTotal`, which respects the category/card filters). Month data is fetched with a `transactionDate` range query (`GetTransactionsUseCase`).
  - **Fatura / Projeção**: projection for a target month (defaults to next month). `GetInvoiceProjectionUseCase` combines two equality queries — `invoiceMonth == target` and `isRecurring == true` (no composite indexes needed). The UI shows projected balance (recurring income − projected invoice − recurring debit expenses), per-card invoice totals ("fecha dia X • vence dia Y"), recurring incomes, and the invoice item list. Recurring CREDIT expenses join the invoice for every month, deduplicated against the `invoiceMonth` query by excluding items whose own `invoiceMonth` equals the target.
- Tapping a transaction (month list, recurring incomes, or invoice items) opens an edit dialog to adjust `title`/`amount` or delete the document (`UpdateTransactionUseCase`/`DeleteTransactionUseCase`). This is how the projected salary is adjusted when the real payment arrives; editing a recurring document changes all future projections.
- Known limitations (current behavior, not bugs to "fix" silently): the new-transaction screen has no date picker (`transactionDate` is always "now"); `status` is always saved as `"pending"`; installment documents created before the split feature are not migrated.

## Data

- Financial data lives in Firestore collections accessed directly from the client with the authenticated user. Any new collection access must be paired with a matching rule update in `web/firestore.rules` (deployed via `firebase deploy --only firestore:rules` from `web\`), otherwise reads/writes fail with `PERMISSION_DENIED`. Current rules: authenticated users can read `transactions`/`cards`/`transactions_category`; create `transactions` (requires `transactionType in ['EXPENSE','INCOME']`) and `cards`; update only `title`/`amount`/`category` of a transaction, where `category` may only shrink (removal via `arrayRemove`); update only `bank`/`paymentMethods`/`finalCode`/`closingDay`/`invoiceDueDate` of a card; create/update/delete `transactions_category` (`title` non-empty string, `description` string, `transactionType` in `['EXPENSE','INCOME']`); and delete `transactions`. All other client writes are denied.
- Backfill for the `transactionType` field: `web/functions/scripts/backfillTransactionType.js` sets `transactionType = "EXPENSE"` on every `transactions_category` and `transactions` document that lacks a valid value (batches of 500, Admin SDK via Application Default Credentials, project from `web/.firebaserc`). Preview with `npm --prefix web\functions run migrate:transaction-type -- --dry-run`, apply with `npm --prefix web\functions run migrate:transaction-type`. It is idempotent; run it once after deploying this feature.
