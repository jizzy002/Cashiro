package com.ritesh.cashiro.presentation.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

private data class WidgetRender(
    val state: WidgetState,
    val accentArgb: Int,
)

@Singleton
class WidgetBalanceObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AccountBalanceRepository,
    private val prefs: UserPreferencesRepository,
    private val updater: WidgetUpdater,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started = false

    fun start() {
        if (started) return
        started = true
        // Hidden accounts live in SharedPreferences (not a flow): without this
        // listener, hiding/unhiding an account would never refresh the widget.
        context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
        scope.launch {
            // Runs the change-driven update loop for as long as the process lives.
            // A failed emission only pauses collection for RETRY_DELAY_MS — with no
            // retry, one transient error would permanently stop widget updates.
            while (true) {
                try {
                    combine(
                        repository.getAllLatestBalances(),
                        prefs.widgetHideBalances,
                        prefs.userPreferences,
                    ) { balances, hide, userPrefs ->
                        WidgetRender(
                            state = WidgetStateBuilder.build(balances, hiddenSet(), hide, LocalDateTime.now()),
                            accentArgb = userPrefs.accentColor.toArgb(),
                        )
                    }
                        .distinctUntilChanged()
                        .collect { render ->
                            updater.updateAll(render.state, render.accentArgb)
                        }
                    break // upstream completed normally
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Widget update flow failed; retrying in ${RETRY_DELAY_MS}ms", e)
                    delay(RETRY_DELAY_MS)
                }
            }
        }
    }

    fun refreshNow(onDone: (() -> Unit)? = null) {
        scope.launch {
            try {
                runCatching { renderOnce() }
                    .onSuccess { render -> updater.updateAll(render.state, render.accentArgb) }
                    .onFailure { Log.w(TAG, "Widget refresh failed", it) }
            } finally {
                onDone?.invoke()
            }
        }
    }

    /** Called from the widget's privacy toggle; the changed pref re-emits through [start]'s flow. */
    fun togglePrivacy(onDone: (() -> Unit)? = null) {
        scope.launch {
            try {
                runCatching {
                    val current = prefs.widgetHideBalances.first()
                    prefs.updateWidgetHideBalances(!current)
                }.onFailure { Log.w(TAG, "Widget privacy toggle failed", it) }
            } finally {
                onDone?.invoke()
            }
        }
    }

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "hidden_accounts") refreshNow()
        }

    private suspend fun renderOnce(): WidgetRender {
        val balances = repository.getAllLatestBalances().first()
        val hide = prefs.widgetHideBalances.first()
        val userPrefs = prefs.userPreferences.first()
        return WidgetRender(
            state = WidgetStateBuilder.build(balances, hiddenSet(), hide, LocalDateTime.now()),
            accentArgb = userPrefs.accentColor.toArgb(),
        )
    }

    private fun hiddenSet(): Set<String> =
        context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
            .getStringSet("hidden_accounts", emptySet()) ?: emptySet()

    private companion object {
        const val TAG = "WidgetBalanceObserver"
        const val RETRY_DELAY_MS = 4_000L
    }
}
