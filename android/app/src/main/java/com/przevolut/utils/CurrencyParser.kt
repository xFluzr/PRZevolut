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

    private val CURRENCY_CODES = setOf("EUR", "USD", "GBP", "CHF", "CZK", "HUF", "SEK", "NOK", "DKK", "UAH", "RON", "TRY", "ISK")

    // Kwota z separatorem dziesiętnym (najwyższy priorytet)
    private val DECIMAL_AMOUNT = """(\d[\d\s]*[.,]\d{1,2})"""
    // Kwota całkowita (2+ cyfry, bez separatora)
    private val INTEGER_AMOUNT = """(\d{2,})"""

    // Symbole walut jako jeden wzorzec
    private val SYMBOL_PATTERN = """([€£$]|Fr|fr|Kč|Kc)"""

    // Kody ISO walut
    private val CODE_PATTERN = """(EUR|USD|GBP|CHF|CZK|HUF|SEK|NOK|DKK|UAH|RON|TRY|ISK)"""

    // --- Wzorce z separatorem dziesiętnym (najwyższy priorytet) ---
    private val SYMBOL_BEFORE_DEC = Regex("""$SYMBOL_PATTERN\s*$DECIMAL_AMOUNT""")
    private val SYMBOL_AFTER_DEC = Regex("""$DECIMAL_AMOUNT\s*$SYMBOL_PATTERN""")
    private val CODE_BEFORE_DEC = Regex("""$CODE_PATTERN\s*$DECIMAL_AMOUNT""")
    private val CODE_AFTER_DEC = Regex("""$DECIMAL_AMOUNT\s*$CODE_PATTERN""")

    // --- Wzorce bez separatora (niższy priorytet) ---
    private val SYMBOL_BEFORE_INT = Regex("""$SYMBOL_PATTERN\s*$INTEGER_AMOUNT""")
    private val SYMBOL_AFTER_INT = Regex("""$INTEGER_AMOUNT\s*$SYMBOL_PATTERN""")
    private val CODE_BEFORE_INT = Regex("""$CODE_PATTERN\s*$INTEGER_AMOUNT""")
    private val CODE_AFTER_INT = Regex("""$INTEGER_AMOUNT\s*$CODE_PATTERN""")

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
        // 1) Najwyższy priorytet: kwoty z jawnym separatorem dziesiętnym
        SYMBOL_BEFORE_DEC.find(line)?.let { m ->
            val symbol = m.groupValues[1]
            val amount = parseAmount(m.groupValues[2]) ?: return@let
            val currency = CURRENCY_SYMBOLS[symbol] ?: return@let
            return Pair(amount, currency)
        }

        SYMBOL_AFTER_DEC.find(line)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            val symbol = m.groupValues[2]
            val currency = CURRENCY_SYMBOLS[symbol] ?: return@let
            return Pair(amount, currency)
        }

        CODE_BEFORE_DEC.find(line)?.let { m ->
            val currency = m.groupValues[1].uppercase()
            val amount = parseAmount(m.groupValues[2]) ?: return@let
            if (currency in CURRENCY_CODES) return Pair(amount, currency)
        }

        CODE_AFTER_DEC.find(line)?.let { m ->
            val amount = parseAmount(m.groupValues[1]) ?: return@let
            val currency = m.groupValues[2].uppercase()
            if (currency in CURRENCY_CODES) return Pair(amount, currency)
        }

        // 2) Niższy priorytet: kwoty bez separatora + heurystyka superscript
        //    Dla symboli walut z 3+ cyframi: wstaw kropkę przed ostatnimi 2 cyframi
        //    np. "$393" → 3.93, "$1488" → 14.88
        //    Dla kodów ISO: traktuj jako liczbę całkowitą (np. "CZK 299" = 299.0)
        SYMBOL_BEFORE_INT.find(line)?.let { m ->
            val symbol = m.groupValues[1]
            val rawAmount = m.groupValues[2]
            val currency = CURRENCY_SYMBOLS[symbol] ?: return@let
            val amount = if (rawAmount.length >= 3) {
                insertDecimalBeforeLast2(rawAmount)
            } else {
                rawAmount.toDoubleOrNull()
            }
            if (amount != null && amount > 0) return Pair(amount, currency)
        }

        SYMBOL_AFTER_INT.find(line)?.let { m ->
            val rawAmount = m.groupValues[1]
            val symbol = m.groupValues[2]
            val currency = CURRENCY_SYMBOLS[symbol] ?: return@let
            val amount = if (rawAmount.length >= 3) {
                insertDecimalBeforeLast2(rawAmount)
            } else {
                rawAmount.toDoubleOrNull()
            }
            if (amount != null && amount > 0) return Pair(amount, currency)
        }

        CODE_BEFORE_INT.find(line)?.let { m ->
            val currency = m.groupValues[1].uppercase()
            val amount = m.groupValues[2].toDoubleOrNull() ?: return@let
            if (currency in CURRENCY_CODES && amount > 0) return Pair(amount, currency)
        }

        CODE_AFTER_INT.find(line)?.let { m ->
            val amount = m.groupValues[1].toDoubleOrNull() ?: return@let
            val currency = m.groupValues[2].uppercase()
            if (currency in CURRENCY_CODES && amount > 0) return Pair(amount, currency)
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

    /**
     * Wstawia kropkę dziesiętną przed ostatnimi 2 cyframi.
     * np. "393" → 3.93, "1488" → 14.88, "698" → 6.98
     */
    private fun insertDecimalBeforeLast2(raw: String): Double? {
        if (raw.length < 3) return null
        val main = raw.substring(0, raw.length - 2)
        val cents = raw.substring(raw.length - 2)
        return "$main.$cents".toDoubleOrNull()
    }
}
