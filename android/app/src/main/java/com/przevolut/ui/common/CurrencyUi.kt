package com.przevolut.ui.common

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.przevolut.R

object CurrencyUi {

    val SUPPORTED = listOf("EUR", "USD", "GBP", "CHF", "CZK")

    private val names = mapOf(
        "EUR" to R.string.currency_eur,
        "USD" to R.string.currency_usd,
        "GBP" to R.string.currency_gbp,
        "CHF" to R.string.currency_chf,
        "CZK" to R.string.currency_czk,
    )

    private val flags = mapOf(
        "EUR" to "🇪🇺",
        "USD" to "🇺🇸",
        "GBP" to "🇬🇧",
        "CHF" to "🇨🇭",
        "CZK" to "🇨🇿",
    )

    private val colors = mapOf(
        "EUR" to R.color.currency_eur,
        "USD" to R.color.currency_usd,
        "GBP" to R.color.currency_gbp,
        "CHF" to R.color.currency_chf,
        "CZK" to R.color.currency_czk,
    )

    @StringRes
    fun nameRes(currency: String): Int = names[currency] ?: R.string.currency_eur

    fun flag(currency: String): String = flags[currency] ?: "💱"

    @ColorRes
    fun colorRes(currency: String): Int = colors[currency] ?: R.color.primary

    fun chipLabel(currency: String): String = "${flag(currency)} $currency"
}
