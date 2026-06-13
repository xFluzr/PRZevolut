package com.przevolut.ui.common

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeHelper {

    private const val PREFS_NAME = "przevolut_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    fun getThemeMode(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, "system") ?: "system"
    }

    fun setThemeMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode)
            .apply()
        applyTheme(mode)
    }

    fun applyTheme(mode: String) {
        val nightMode = when (mode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    /** Przełącza między jasnym a ciemnym trybem. Zwraca nowy tryb. */
    fun toggleLightDark(context: Context): String {
        val isDark = when (getThemeMode(context)) {
            "dark" -> true
            "light" -> false
            else -> {
                val nightModeFlags = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
        val newMode = if (isDark) "light" else "dark"
        setThemeMode(context, newMode)
        return newMode
    }

    fun isDarkMode(context: Context): Boolean {
        return when (getThemeMode(context)) {
            "dark" -> true
            "light" -> false
            else -> {
                val nightModeFlags = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
}
