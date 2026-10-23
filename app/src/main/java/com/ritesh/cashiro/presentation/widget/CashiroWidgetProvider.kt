package com.ritesh.cashiro.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors

class CashiroWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        entry.widgetBalanceObserver().refreshNow()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_TOGGLE_PRIVACY) {
            val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            entry.widgetBalanceObserver().togglePrivacy()
        }
    }

    companion object {
        const val ACTION_TOGGLE_PRIVACY = "com.ritesh.cashiro.action.WIDGET_TOGGLE_PRIVACY"
    }
}
