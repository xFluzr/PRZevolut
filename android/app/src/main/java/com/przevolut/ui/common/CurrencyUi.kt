package com.przevolut.ui.common

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.przevolut.R

object CurrencyUi {

    val SUPPORTED = listOf("EUR", "USD", "GBP", "CHF", "CZK", "HUF", "UAH", "DKK", "ISK", "NOK", "SEK", "RON", "TRY")

    private val names = mapOf(
        "EUR" to R.string.currency_eur,
        "USD" to R.string.currency_usd,
        "GBP" to R.string.currency_gbp,
        "CHF" to R.string.currency_chf,
        "CZK" to R.string.currency_czk,
        "HUF" to R.string.currency_huf,
        "UAH" to R.string.currency_uah,
        "DKK" to R.string.currency_dkk,
        "ISK" to R.string.currency_isk,
        "NOK" to R.string.currency_nok,
        "SEK" to R.string.currency_sek,
        "RON" to R.string.currency_ron,
        "TRY" to R.string.currency_try,
    )

    private val flags = mapOf(
        "EUR" to "🇪🇺",
        "USD" to "🇺🇸",
        "GBP" to "🇬🇧",
        "CHF" to "🇨🇭",
        "CZK" to "🇨🇿",
        "HUF" to "🇭🇺",
        "UAH" to "🇺🇦",
        "DKK" to "🇩🇰",
        "ISK" to "🇮🇸",
        "NOK" to "🇳🇴",
        "SEK" to "🇸🇪",
        "RON" to "🇷🇴",
        "TRY" to "🇹🇷",
    )

    private val colors = mapOf(
        "EUR" to R.color.currency_eur,
        "USD" to R.color.currency_usd,
        "GBP" to R.color.currency_gbp,
        "CHF" to R.color.currency_chf,
        "CZK" to R.color.currency_czk,
        "HUF" to R.color.currency_huf,
        "UAH" to R.color.currency_uah,
        "DKK" to R.color.currency_dkk,
        "ISK" to R.color.currency_isk,
        "NOK" to R.color.currency_nok,
        "SEK" to R.color.currency_sek,
        "RON" to R.color.currency_ron,
        "TRY" to R.color.currency_try,
    )

    @StringRes
    fun nameRes(currency: String): Int = names[currency] ?: R.string.currency_eur

    fun flag(currency: String): String = flags[currency] ?: "💱"

    @ColorRes
    fun colorRes(currency: String): Int = colors[currency] ?: R.color.primary

    fun chipLabel(currency: String): String = "${flag(currency)} $currency"
}
