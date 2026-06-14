package com.przevolut.ui.common

import android.content.Context
import android.content.res.Configuration

/**
 * Centralny punkt zarządzania ustawieniami dostępności.
 *
 * Przechowuje preferencje (rozmiar tekstu)
 * w SharedPreferences i dostarcza metody do ich odczytu.
 */
object AccessibilityHelper {

    private const val PREFS_NAME = "przevolut_prefs"
    private const val KEY_FONT_SCALE = "a11y_font_scale"

    // ── Font scale ─────────────────────────────────────────────────────

    fun getFontScale(context: Context): Float {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_FONT_SCALE, 1.0f)
    }

    fun setFontScale(context: Context, scale: Float) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_FONT_SCALE, scale).apply()
    }

    // ── Font scale application ─────────────────────────────────────────

    /**
     * Tworzy nowy Context z nadpisanym fontScale.
     * Wywoływać z Activity.attachBaseContext().
     */
    fun applyFontScale(baseContext: Context): Context {
        val scale = getFontScale(baseContext)
        if (scale == 1.0f) return baseContext

        val config = Configuration(baseContext.resources.configuration)
        config.fontScale = scale
        return baseContext.createConfigurationContext(config)
    }
}
