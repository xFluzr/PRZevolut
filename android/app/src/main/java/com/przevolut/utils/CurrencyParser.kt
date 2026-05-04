package com.przevolut.utils

/**
 * Parser kwot walutowych z tekstu OCR.
 *
 * Obsługuje wzorce:
 * - "€ 12,99" / "€12.99" / "12,99 €"
 * - "12.99 EUR" / "USD 49.99" / "£ 5.50"
 * - "CHF 3,20" / "CZK 299"
 */
object CurrencyParser {

    private val CURRENCY_SYMBOLS = mapOf(
        "€" to "EUR",
        "$" to "USD",
        "£" to "GBP",
        "Fr" to "CHF",
        "Kč" to "CZK",
    )

    private val CURRENCY_CODES = setOf("EUR", "USD", "GBP", "CHF", "CZK")

    /**
     * Próbuje sparsować kwotę i walutę z podanego tekstu OCR.
     * @return Para (kwota: Double, waluta: String) lub null jeśli nie wykryto.
     */
    fun parse(text: String): Pair<Double, String>? {
        // Wzorzec: symbol+kwota lub kwota+symbol, ze spacją lub bez
        // Przykłady: "€12,99", "12.99€", "€ 12,99", "EUR 12.99", "12.99 EUR"
        val combinedPattern = Regex(
            """([€$£]|Fr|Kč)\s*([\d]+[.,][\d]{1,2}|[\d]+)|([\d]+[.,][\d]{1,2}|[\d]+)\s*([€$£]|Fr|Kč|EUR|USD|GBP|CHF|CZK)"""
        )

        val match = combinedPattern.find(text) ?: return null

        val (symbolBefore, amountBefore, amountAfter, symbolAfter) = match.destructured

        val rawAmount = (amountBefore.ifBlank { amountAfter })
            .replace(',', '.') // normalize comma to dot
        val rawSymbol = symbolBefore.ifBlank { symbolAfter }

        val amount = rawAmount.toDoubleOrNull() ?: return null
        val currency = CURRENCY_SYMBOLS[rawSymbol] ?: rawSymbol.uppercase().takeIf { it in CURRENCY_CODES }
        ?: return null

        return Pair(amount, currency)
    }
}
