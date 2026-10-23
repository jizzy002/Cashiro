package com.ritesh.cashiro.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ritesh.cashiro.MainActivity
import com.ritesh.cashiro.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)
    private val density = context.resources.displayMetrics.density

    fun updateAll(state: WidgetState, accentArgb: Int) {
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, CashiroWidgetProvider::class.java))
        ids.forEach { update(it, state, accentArgb) }
    }

    fun update(appWidgetId: Int, state: WidgetState, accentArgb: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_ledger)
        applyState(views, state, accentArgb)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun px(dp: Int) = (dp * density).toInt().coerceAtLeast(1)

    private fun applyState(views: RemoteViews, state: WidgetState, accentArgb: Int) {
        // NOTE: only @RemotableViewMethod-annotated APIs are usable here.
        // setBackgroundResource/background tint are NOT remotable — they abort the
        // whole RemoteViews update, leaving the widget stuck on its initialLayout.
        // Card background therefore lives in the layout XML only.

        // Header + amount (fonts/sizes come from the layout XML)
        views.setTextViewText(R.id.total_label, context.getString(R.string.widget_total).uppercase())
        views.setTextViewText(
            R.id.total_amount,
            if (state.privacyHidden) MASK else state.totalText,
        )
        views.setTextViewText(
            R.id.updated_text,
            context.getString(R.string.widget_updated, state.updatedText.removePrefix("Updated ")),
        )

        // + button drawn as a circle bitmap (RemoteViews background tint would square the shape)
        views.setImageViewBitmap(R.id.add_button, WidgetDrawer.plus(accentArgb, px(30)))
        val addIntent = Intent(context, MainActivity::class.java).apply { action = MainActivity.ACTION_ADD_TRANSACTION }
        views.setOnClickPendingIntent(
            R.id.add_button,
            PendingIntent.getActivity(
                context, 0, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )

        // Privacy toggle on the total area (tap) and the eye icon (explicit button)
        val togglePi = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, CashiroWidgetProvider::class.java).apply { action = CashiroWidgetProvider.ACTION_TOGGLE_PRIVACY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.total_amount, togglePi)
        views.setOnClickPendingIntent(R.id.total_label, togglePi)
        // Eye reflects the state: crossed when balances are masked
        views.setImageViewResource(
            R.id.privacy_toggle,
            if (state.privacyHidden) R.drawable.ic_widget_eye_off else R.drawable.ic_widget_eye,
        )
        views.setOnClickPendingIntent(R.id.privacy_toggle, togglePi)

        // Rows
        views.removeAllViews(R.id.rows_container)
        state.accounts.forEachIndexed { index, row ->
            val rowViews = RemoteViews(context.packageName, R.layout.widget_ledger_row)
            rowViews.setImageViewBitmap(R.id.bank_dot, WidgetDrawer.dot(row.bankName, row.dotColorArgb, px(24)))
            rowViews.setTextViewText(R.id.bank_name, row.bankName)
            if (Regex("\\d{4}").matches(row.last4)) {
                rowViews.setViewVisibility(R.id.account_last4, android.view.View.VISIBLE)
                rowViews.setTextViewText(R.id.account_last4, "···· ${row.last4}")
            } else {
                rowViews.setViewVisibility(R.id.account_last4, android.view.View.GONE)
            }
            rowViews.setTextViewText(
                R.id.account_amount,
                if (state.privacyHidden) MASK else row.amountText,
            )
            val openIntent = Intent(context, MainActivity::class.java).apply {
                action = MainActivity.ACTION_OPEN_ACCOUNT
                putExtra(MainActivity.EXTRA_BANK_NAME, row.bankName)
                putExtra(MainActivity.EXTRA_ACCOUNT_LAST4, row.last4)
            }
            rowViews.setOnClickPendingIntent(
                R.id.row_container,
                PendingIntent.getActivity(
                    context, 100 + index, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            views.addView(R.id.rows_container, rowViews)
        }
    }

    companion object {
        private const val MASK = "••••••"
    }
}
