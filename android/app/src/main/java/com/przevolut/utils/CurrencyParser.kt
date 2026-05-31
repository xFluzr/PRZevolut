package com.przevolut.utils

/**
 * Parser kwot walutowych z tekstu OCR.
 *
 * Obsługuje wzorce:
 * - "€ 12,99" / "€12.99" / "12,99 €"
 * - "12.99 EUR" / "USD 49.99" / "£ 5.50"
 * - "CHF 3,20" / "CZK 299" / "$ 1 234.56"
 *
 * Strategia: wymagamy separatora dziesiętnego (przecinek lub kropka) LUB jawnego kodu/symbolu
 * waluty przy liczbie — żeby uniknąć fałszywych trafień na daty, numery domów itp.
 */
object CurrencyParser {

    private val CURRENCY_SYMBOLS = mapOf(
        "€" to "EUR",
        "$" to "USD",
        "\$" to "USD",
        "£" to "GBP",
        "Fr" to "CHF",
        "fr" to "CHF",
        "Kč" to "CZK",
        "Kc" to "CZK", // OCR czasem myli "č"
    )

    private val CURRENCY_CODES = setOf("EUR", "USD", "GBP", "CHF", "CZK", "HUF", "SEK", "NOK", "DKK")

    // Kwota: liczba z separatorem dziesiętnym (obowiązkowym) lub liczba całkowita >= 2 cyfry
    // Grupowanie tysięcy (spacja / kropka / przecinek) obsługiwane opcjonalnie
    private val AMOUNT_PATTERN = """(\d[\d\s]*[.,]\d{1,2}|\d{2,})"""

    // Symbole walut jako jeden wzorzec
    private val SYMBOL_PATTERN = """([€£$]|Fr|fr|Kč|Kc)"""

    // Kody ISO walut
    private val CODE_PATTERN = """(EUR|USD|GBP|CHF|CZK|HUF|SEK|NOK|DKK)"""

    // Wzorzec 1: symbol przed kwotą — "€12,99" / "$ 49.99"
    private val SYMBOL_BEFORE = Regex("""$SYMBOL_PATTERN\s*$AMOUNT_PATTERN""")

    // Wzorzec 2: kwota przed symbolem — "12,99€" / "5.50 £"
    private val SYMBOL_AFTER = Regex("""$AMOUNT_PATTERN\s*$SYMBOL_PATTERN""")

    // Wzorzec 3: kod ISO przed kwotą — "EUR 12.99" / "USD49.99"
    private val CODE_BEFORE = Regex("""$CODE_PATTERN\s*$AMOUNT_PATTERN""")

    // Wzorzec 4: kwota przed kodem ISO — "12.99 EUR" / "49,99EUR"
    private val CODE_AFTER = Regex("""$AMOUNT_PATTERN\s*$CODE_PATTERN""")

    /**
     * Próbuje sparsować kwotę i walutę z podanego tekstu OCR.
     * Przeszukuje linia po linii — bierze pierwsze znalezione dopasowanie.
     * @return Para (kwota: Double, waluta: String) lub null jeśli nie wykryto.
     */
    fun parse(text: String): Pair<Double, String>? {
        // Sprawdzaj linia po linii — OCR często zwraca wieloliniowy tekst
        for (line in text.lines()) {
            val result = parseLine(line.trim()) ?: continue
            return result
        }
        return null
    }

    private fun parseLine(line: String): Pair<Double, String>? {
        // Próbuj wzorce w kolejności od najbardziej specyficznego
        SYMBOL_BEFORE.find(line)?.let { m ->
            val symbol = m.groupValues[1]
            val amount = parseAmount(m.groupValues[2]) ?: return@let
            val currency = CURRENCY_SYMBOLS[symbol] ?: return@let
            return Pair(amount, currency)
        }

        SYMBOL_AFTER.find(line)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            val symbol = m.groupValues[2]
            val currency = CURRENCY_SYMBOLS[symbol] ?: return@let
            return Pair(amount, currency)
        }

        CODE_BEFORE.find(line)?.let { m ->
            val currency = m.groupValues[1].uppercase()
            val amount = parseAmount(m.groupValues[2]) ?: return@let
            if (currency in CURRENCY_CODES) return Pair(amount, currency)
        }

        CODE_AFTER.find(line)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            val currency = m.groupValues[2].uppercase()
            if (currency in CURRENCY_CODES) return Pair(amount, currency)
        }

        return null
    }

    private fun parseAmount(raw: String): Double? {
        // Usuń spacje (separatory tysięcy), zamień przecinek na kropkę
        val normalized = raw.replace("\\s".toRegex(), "").replace(',', '.')
        // Jeśli jest więcej niż jedna kropka (np. "1.234.56" → błąd), odrzuć
        if (normalized.count { it == '.' } > 1) return null
        return normalized.toDoubleOrNull()
    }
}
