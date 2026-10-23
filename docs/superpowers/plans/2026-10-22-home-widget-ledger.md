# Home Widget (Design A — Ledger) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a 4×2 home-screen widget (Design A — Ledger) showing total balance + up to 3 accounts, an accent `+` button deep-linking to Add Transaction, per-row deep links to Account Detail, privacy-hide toggle, and mixed-currency total (`—`). Battery-efficient, event-driven refresh.

**Architecture:** Classic `AppWidgetProvider` + `RemoteViews` (no Glance dependency). A `@Singleton WidgetBalanceObserver` collects `AccountBalanceRepository.getAllLatestBalances()` plus DataStore preferences on an application scope and re-renders all widget instances only when the combined state changes. Balance writes (SMS path via `BalanceUpdateProcessor.process`, manual path via `AddTransactionUseCase.execute`) flow through Room, which emits to the collector — no periodic WorkManager, no alarms, no wake-locks. Tap handling uses explicit-intent extras through `MainActivity.handleIntent` (the proven shortcut pattern).

**Tech Stack:** Kotlin 2.3, Hilt 2.58, Room 2.8.4, DataStore 1.2.0, WorkManager 2.11.0 (already present, used only as the Hilt worker factory host — not for polling), AppWidget `RemoteViews`, `PendingIntent`, `SharedPreferences` ("account_prefs"/"hidden_accounts").

**Spec:** `docs/superpowers/designs/widget-mockup.html` (Design A) + `docs/superpowers/designs/widget-a-picture-dark.png`.

## Global Constraints

- Package `com.ritesh.cashiro`; minSdk 26; no new dependencies (use Glance-less RemoteViews).
- Widget size: 4×2 cells (`minWidth=250dp`, `minHeight=110dp`, `resizeMode=horizontal|vertical`, `widgetCategory=home_screen`).
- Amounts use `CurrencyFormatter.formatCurrency(amount, code)` (Bosnian-style "5.482,17 KM") — do not reimplement.
- Hidden accounts (`account_prefs` / `hidden_accounts`) are excluded from the widget list.
- Privacy-hide is a new DataStore boolean key `WIDGET_HIDE_BALANCES` (default `false`); toggled by tapping the total amount.
- Mixed currency: if the visible accounts span more than one currency code, total shows `—` (em dash).
- Max 3 account rows; pick the 3 most recently updated, excluding hidden accounts.
- No `updatePeriodMillis` (set 0); refresh only on balance change, preference change, privacy toggle, and `ACTION_WALLPAPER_CHANGED`/`ui-mode` via `onUpdate` triggers.
- All widget strings localized via `res/values/strings.xml` (English base) so translations apply.
- Tests: pure JUnit4 under `app/src/test`; no Robolectric. Widget state logic is pure and must be unit-tested.

## Review Focus

- **Hidden accounts leaked**: a hidden account appears on the widget because filtering uses a stale snapshot of `hidden_accounts` → test `WidgetStateBuilder` with a hidden set that excludes it.
- **Mixed currency total still sums**: the total shows a number when accounts are in different currencies → assert `"—"` when distinct count > 1.
- **Privacy toggle breaks mixed state**: unhiding after mixed currency shows the summed number instead of `—` → test `render` composition (hide + mixed) returns the em dash.
- **Stale relative time**: "Updated 2 min ago" ticks incorrectly → use a static wall-clock timestamp ("Updated HH:mm") to avoid stale text.
- **Account identity mismatch**: `AccountDetail` is keyed by (bankName, last4); a widget tap that passes the wrong pair opens the wrong account → map strictly from the same `AccountBalanceEntity` used to render.

---

## File Structure

- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/WidgetState.kt` — pure data + builder + tests.
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/CashiroWidgetProvider.kt` — `AppWidgetProvider`, RemoteViews, PendingIntents, broadcast handling.
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/WidgetUpdater.kt` — `@Singleton` renders state into RemoteViews and calls `AppWidgetManager.updateAppWidget` for all instances.
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/WidgetBalanceObserver.kt` — `@Singleton` collector that combines balances + prefs + hidden set and triggers `WidgetUpdater`.
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/WidgetEntryPoint.kt` — `@EntryPoint` for context-only access from the receiver.
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/AccentColorExtensions.kt` — `AccentColor.toArgb()` mapping.
- Create: `app/src/main/res/layout/widget_ledger.xml`, `widget_ledger_row.xml`, `res/xml/widget_ledger_info.xml`, `res/drawable/widget_card_bg.xml`.
- Modify: `app/src/main/AndroidManifest.xml` — register receiver + intent filter.
- Modify: `app/src/main/java/com/ritesh/cashiro/CashiroApplication.kt` — start `WidgetBalanceObserver` in `onCreate`.
- Modify: `app/src/main/java/com/ritesh/cashiro/MainActivity.kt` — `ACTION_OPEN_ACCOUNT` + extras handling.
- Modify: `app/src/main/java/com/ritesh/cashiro/CashiroApp.kt` — navigate to `AccountDetail` on the new action.
- Modify: `app/src/main/java/com/ritesh/cashiro/data/preferences/UserPreferencesRepository.kt` — add `WIDGET_HIDE_BALANCES` key + Flow + writer.
- Modify: `app/src/main/res/values/strings.xml` — widget strings.
- Test: `app/src/test/java/com/ritesh/cashiro/presentation/widget/WidgetStateBuilderTest.kt`.

## Task 1: WidgetState (pure) + tests

**Files:**
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/WidgetState.kt`
- Test: `app/src/test/java/com/ritesh/cashiro/presentation/widget/WidgetStateBuilderTest.kt`

**Interfaces:**
- Produces: `data class WidgetState(val totalText: String, val updatedText: String, val privacyHidden: Boolean, val accounts: List<WidgetAccountRow>)`, `data class WidgetAccountRow(val bankName: String, val last4: String, val amountText: String)`, `object WidgetStateBuilder { fun build(balances: List<AccountBalanceEntity>, hiddenAccounts: Set<String>, privacyHidden: Boolean, baseCurrency: String?, now: LocalDateTime): WidgetState }`.

- [ ] **Step 1: Write failing test** in `WidgetStateBuilderTest.kt` covering: (a) 3 most-recent non-hidden accounts, (b) total sums when single currency, (c) `—` when mixed, (d) privacy flag passed through, (e) hidden account excluded, (f) empty list → total `—`/`0`? (decide: empty → total `"0,00 KM"`? Use `CurrencyFormatter.formatCurrency(BigDecimal.ZERO, "BAM")` only if base currency BAM; otherwise use first account's currency or "INR". Simpler: if no accounts, total `"—"`.)

Test code (JUnit4, no Robolectric):
```kotlin
class WidgetStateBuilderTest {
  private fun bal(bank: String, last4: String, amount: String, currency: String, ts: Long) =
    AccountBalanceEntity(id=0, iconResId=0, iconName="", bankName=bank, accountLast4=last4,
      balance=BigDecimal(amount), timestamp=LocalDateTime.ofEpochSecond(ts,0,ZoneOffset.UTC),
      currency=currency, isCreditCard=false, isWallet=false)

  @Test fun `mixed currencies total is em dash`() {
    val s = WidgetStateBuilder.build(listOf(
      bal("Raiffeisen","1234","2340.50","BAM",100),
      bal("UniCredit","5678","1890.67","EUR",90)), emptySet(), false, "BAM", LocalDateTime.now())
    assertEquals("—", s.totalText)
  }
  @Test fun `single currency total sums`() { ... assertEquals("4.231,17 KM", s.totalText) ... }
  @Test fun `hidden account excluded`() { ... assertEquals(1, s.accounts.size) ... }
  @Test fun `privacy flag carried`() { assertTrue(s.privacyHidden) }
}
```
(Adjust expected strings to match CurrencyFormatter output; test will pin it.)

- [ ] **Step 2: Run test** → FAIL (class not found).
- [ ] **Step 3: Implement** `WidgetState.kt`:
```kotlin
package com.ritesh.cashiro.presentation.widget
import com.ritesh.cashiro.data.database.entity.AccountBalanceEntity
import com.ritesh.cashiro.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class WidgetAccountRow(val bankName: String, val last4: String, val amountText: String)

data class WidgetState(
  val totalText: String,
  val updatedText: String,
  val privacyHidden: Boolean,
  val accounts: List<WidgetAccountRow>,
)

object WidgetStateBuilder {
  fun build(balances, hiddenAccounts, privacyHidden, now): WidgetState {
    val visible = balances
      .filter { "${it.bankName}_${it.accountLast4}" !in hiddenAccounts }
      .sortedByDescending { it.timestamp }
      .take(3)
    val currencies = visible.map { it.currency }.distinct()
    val totalText = if (visible.isEmpty()) "—" else if (currencies.size > 1) "—" else
      CurrencyFormatter.formatCurrency(visible.sumOf { it.balance }, currencies.first())
    val rows = visible.map { WidgetAccountRow(it.bankName, it.accountLast4,
      CurrencyFormatter.formatCurrency(it.balance, it.currency)) }
    val updatedText = "Updated " + now.format(DateTimeFormatter.ofPattern("HH:mm"))
    return WidgetState(totalText, updatedText, privacyHidden, rows)
  }
}
```
- [ ] **Step 4: Run tests** → PASS.
- [ ] **Step 5: Commit** `feat(widget): pure WidgetState builder`.

## Task 2: Resources (layouts, provider info, drawables, strings)

**Files:**
- Create: `app/src/main/res/xml/widget_ledger_info.xml`
- Create: `app/src/main/res/layout/widget_ledger.xml`
- Create: `app/src/main/res/layout/widget_ledger_row.xml`
- Create: `app/src/main/res/drawable/widget_card_bg.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add appwidget-provider XML** with `minWidth=250dp`, `minHeight=110dp`, `updatePeriodMillis=0`, `initialLayout=@layout/widget_ledger`, `resizeMode=horizontal|vertical`, `widgetCategory=home_screen`, `previewLayout=@layout/widget_ledger` (API 31+).
- [ ] **Step 2: Add layouts** using RemoteViews-compatible views only (LinearLayout, TextView, ImageView, FrameLayout). Root `LinearLayout` vertical, card bg. Header row: "TOTAL" label (10sp, uppercase, muted) + `+` ImageView (30dp circle, accent bg). Big amount TextView 28sp bold. Row container `LinearLayout` vertical with 3 `include`s of `widget_ledger_row` (each row: bank dot ImageView 24dp circle, name 13sp, last4 10sp, amount 13sp right). Footer "Updated %1$s" 9sp muted. Use `@color`? RemoteViews can't reference theme; use hardcoded hex `#1E1F25` bg, `#E2E2E9` text, `#9A9DA8` muted, `#282A2F` divider. Accent applied programmatically to the `+` button.
- [ ] **Step 3: Strings** add `widget_total`, `widget_updated`, `widget_add_transaction`, `widget_open_account`.
- [ ] **Step 4: Commit** `feat(widget): resources and layouts`.

## Task 3: UserPreferencesRepository — privacy key

**Files:**
- Modify: `app/src/main/java/com/ritesh/cashiro/data/preferences/UserPreferencesRepository.kt`

- [ ] Add `WIDGET_HIDE_BALANCES = booleanPreferencesKey("widget_hide_balances")` in `PreferencesKeys` object.
- [ ] Add `val widgetHideBalances: Flow<Boolean>` (default false) and `suspend fun updateWidgetHideBalances(enabled: Boolean)`.
- [ ] Commit.

## Task 4: AccentColor → ARGB

**Files:**
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/AccentColorExtensions.kt`

- [ ] `fun AccentColor.toArgb(): Int` mapping each enum value to the corresponding `Color.kt` primary (Latte_* for light; use Macchiato_*_dim? For widget dark card, use Macchiato_*_dim or Latte? Pick the bright Latte primaries to match the in-app accent default; mockup used `#007AFF` for BLUE → `Latte_Blue` = 0xFF007AFF). Map 24 values. Commit.

## Task 5: MainActivity deep links

**Files:**
- Modify: `app/src/main/java/com/ritesh/cashiro/MainActivity.kt`
- Modify: `app/src/main/java/com/ritesh/cashiro/CashiroApp.kt`

- [ ] Add `const val ACTION_OPEN_ACCOUNT = "com.ritesh.cashiro.action.OPEN_ACCOUNT"` and `EXTRA_BANK_NAME`, `EXTRA_ACCOUNT_LAST4`.
- [ ] In `handleIntent()`, parse extras when action == `ACTION_OPEN_ACCOUNT` into `openAccountBankName`/`openAccountLast4` state.
- [ ] In `CashiroApp.kt`, add `LaunchedEffect(openAccountBankName, openAccountLast4)` navigating `AccountDetail(bankName, last4)` when both non-null.
- [ ] Commit.

## Task 6: WidgetUpdater

**Files:**
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/WidgetUpdater.kt`

- [ ] `@Singleton class WidgetUpdater @Inject constructor(@ApplicationContext context, appWidgetManager: AppWidgetManager, entryPoint: ...)` — simpler: `@Singleton class WidgetUpdater @Inject constructor(@ApplicationContext private val context: Context)`; obtains `AppWidgetManager` internally.
- [ ] `fun updateAll(state: WidgetState)` and `fun update(appWidgetId: Int, state: WidgetState)`:
  - Build RemoteViews from `R.layout.widget_ledger`.
  - Set total text (or dots if privacyHidden), updated text.
  - Set `+` button tint to accent ARGB; PendingIntent to MainActivity with `ACTION_ADD_TRANSACTION`.
  - For each of 3 rows: bind account row data via `RemoteViews` nested layout (`addView(R.id.rows_container, rowViews)`), set text + click PendingIntent with `ACTION_OPEN_ACCOUNT` + extras.
  - If fewer than 3 accounts, hide remaining rows (`setViewVisibility(..., GONE)`).
  - Apply privacy: if hidden, set all amount texts to `••••••`.
  - `appWidgetManager.updateAppWidget(ids, views)`.
- [ ] Commit.

## Task 7: CashiroWidgetProvider

**Files:**
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/CashiroWidgetProvider.kt`

- [ ] `class CashiroWidgetProvider : AppWidgetProvider()`:
  - `onUpdate(context, appWidgetManager, appWidgetIds)`: trigger one immediate render via `WidgetBalanceObserver.refreshNow()` (or entry point) + schedule observer if not started.
  - `onReceive`: handle `ACTION_WIDGET_TOGGLE_PRIVACY` broadcast → flip `WIDGET_HIDE_BALANCES` via entry point, then refresh.
  - `onDeleted`, `onDisabled`: cleanup if needed.
- [ ] `@AndroidEntryPoint`? Receivers with Hilt: use `EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)` instead (existing receivers use statics; keep consistent).
- [ ] Commit.

## Task 8: WidgetEntryPoint

**Files:**
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/WidgetEntryPoint.kt`

- [ ] `@EntryPoint @InstallIn(SingletonComponent::class) interface WidgetEntryPoint { fun accountBalanceRepository(): AccountBalanceRepository; fun userPreferencesRepository(): UserPreferencesRepository; fun widgetUpdater(): WidgetUpdater; fun hiddenAccountsPrefs(): SharedPreferences? }` — SharedPreferences not provided by Hilt; instead expose a helper `fun context(): Context` and read prefs directly in observer. Simpler: expose `Context`.
- [ ] Commit.

## Task 9: WidgetBalanceObserver

**Files:**
- Create: `app/src/main/java/com/ritesh/cashiro/presentation/widget/WidgetBalanceObserver.kt`

- [ ] `@Singleton class WidgetBalanceObserver @Inject constructor(@ApplicationContext context, repo, prefs, updater, hiddenAccountsReader)`:
  - Own `CoroutineScope(SupervisorJob() + Dispatchers.Default)`.
  - `fun start()` launches `combine(repo.getAllLatestBalances(), prefs.widgetHideBalances, prefs.accentColor, prefs.isAmoledMode) { ... }.distinctUntilChanged().collect { state -> updater.updateAll(state) }`.
  - Hidden accounts read from `context.getSharedPreferences("account_prefs", MODE_PRIVATE).getStringSet("hidden_accounts", emptySet())` inside the combine transform.
  - `fun refreshNow()` calls a single render with current values (one-shot).
- [ ] Commit.

## Task 10: Wire observer + manifest

**Files:**
- Modify: `CashiroApplication.kt` — inject `WidgetBalanceObserver`, call `start()` in `onCreate` (guard `:crash` process, existing guard).
- Modify: `AndroidManifest.xml` — add receiver:
```xml
<receiver android:name=".presentation.widget.CashiroWidgetProvider"
  android:exported="true">
  <intent-filter>
    <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    <action android:name="com.ritesh.cashiro.action.WIDGET_TOGGLE_PRIVACY" />
  </intent-filter>
  <meta-data android:name="android.appwidget.provider" android:resource="@xml/widget_ledger_info" />
</receiver>
```
- Commit.

## Task 11: Verification

- [ ] `./gradlew :app:assembleDebug` passes.
- [ ] `./gradlew :app:testDebugUnitTest` passes (WidgetStateBuilderTest).
- [ ] Install APK on device; add widget; verify layout matches picture; tap `+` opens Add Transaction; tap row opens Account Detail; mixed currency shows `—`; privacy toggle works; hidden accounts excluded; SMS/manual add updates widget within ~1s; no crash on process restart; battery: no WorkManager periodic (check `adb shell dumpsys jobscheduler` no new periodic job for the app).
- [ ] Commit any fixes.

## Execution Handoff

Plan complete. Please review. Which execution approach?

- Subagent-driven (recommended — many independent files, clear interfaces)
- Native (this session)

For this plan I recommend Subagent-driven because tasks have well-defined interfaces and independent files, and a shipped mistake in a widget (crashes on home screen) is costly. Does the plan capture what you want, and which approach should we use?
```<|end|><|start|><|start|>assistant<|channel|>final<|message|>I've drafted the full implementation plan and saved it at:

`docs/superpowers/plans/2026-10-22-home-widget-ledger.md`

It covers 11 tasks: pure `WidgetState` model + tests, resources, preferences privacy key, accent mapping, deep links, `WidgetUpdater`, provider, entry point, balance observer, manifest/Application wiring, and verification. Key design decisions locked in:
- RemoteViews (no Glance dependency) — matches the existing Compose-only app without new deps.
- Event-driven refresh via a `@Singleton` observer on `getAllLatestBalances()` — no `updatePeriodMillis`, no alarms, no wake-locks.
- Privacy toggled by tapping the total amount; mixed currency → `—`.
- Account rows keyed strictly by `(bankName, last4)` to match `AccountDetail`.

Please review the plan. Which execution approach would you prefer?

- **Subagent-driven** — fresh subagent per task with reviewer gates (recommended; many independent files, and a widget crash on the home screen is costly)
- **Native** — I implement all tasks in this session, then one full review at the end

Does the plan capture what you want, and which approach should we use? (If you already have a preference, say "Native" or "Subagent-driven" and I'll start.)<|end|><|start|>user<|channel|>continue with the build