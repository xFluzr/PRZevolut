package com.przevolut.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardWatchlistStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getWatchedCurrencies(): Set<String> {
        val saved = prefs.getStringSet(KEY_CURRENCIES, null)
        return saved?.toSet() ?: DEFAULT_CURRENCIES
    }

    fun saveWatchedCurrencies(currencies: Set<String>) {
        val normalized = if (currencies.isEmpty()) DEFAULT_CURRENCIES else currencies
        prefs.edit { putStringSet(KEY_CURRENCIES, normalized) }
    }

    companion object {
        private const val PREFS_NAME = "dashboard_prefs"
        private const val KEY_CURRENCIES = "watched_currencies"
        val DEFAULT_CURRENCIES = setOf("EUR", "USD")
    }
}
