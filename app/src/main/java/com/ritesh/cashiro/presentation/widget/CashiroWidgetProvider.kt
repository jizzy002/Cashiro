package com.ritesh.cashiro.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors

class CashiroWidgetProvider : AppWidgetProvider() {

    /**
     * goAsync() keeps the process alive (system receivers may otherwise kill it)
     * while our coroutines do the actual DataStore/Room work; the observer calls
     * finish() when done. Any synchronous failure still finishes the call so the
     * system never waits out the 10-second timeout for nothing.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        try {
            val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            entry.widgetBalanceObserver().refreshNow(onDone = pending::finish)
        } catch (t: Throwable) {
            android.util.Log.w("CashiroWidgetProvider", "Widget onUpdate failed", t)
            pending.finish()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_PRIVACY) {
            super.onReceive(context, intent)
            return
        }
        val pending = goAsync()
        try {
            val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            entry.widgetBalanceObserver().togglePrivacy(onDone = pending::finish)
        } catch (t: Throwable) {
            android.util.Log.w("CashiroWidgetProvider", "Widget toggle failed", t)
            pending.finish()
        }
    }

    companion object {
        const val ACTION_TOGGLE_PRIVACY = "com.ritesh.cashiro.action.WIDGET_TOGGLE_PRIVACY"
    }
}
