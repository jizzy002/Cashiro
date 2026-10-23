package com.ritesh.cashiro.presentation.widget

import android.content.Context
import android.content.SharedPreferences
import com.ritesh.cashiro.data.preferences.UserPreferencesRepository
import com.ritesh.cashiro.data.repository.AccountBalanceRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun accountBalanceRepository(): AccountBalanceRepository
    fun userPreferencesRepository(): UserPreferencesRepository
    fun widgetUpdater(): WidgetUpdater
    fun widgetBalanceObserver(): WidgetBalanceObserver
}
