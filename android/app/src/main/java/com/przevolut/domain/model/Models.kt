package com.przevolut.domain.model

/**
 * Model domeny — kurs waluty.
 * Niezależny od warstwy danych (Room/Retrofit).
 */
data class ExchangeRate(
    val currency: String,       // Kod ISO waluty: "EUR", "USD", "GBP", "CHF", "CZK"
    val rate: Double,           // Kurs do PLN
    val mid: Double,
    val effectiveDate: String,  // "2026-05-04"
    val fetchedAt: Long,        // Unix timestamp w milisekundach
)

/**
 * Model domeny — alert walutowy.
 */
data class CurrencyAlert(
    val id: Int,
    val currency: String,
    val direction: AlertDirection,
    val targetRate: Double,
    val isActive: Boolean,
    val isTriggered: Boolean,
)

enum class AlertDirection(val value: String) {
    BELOW("below"),
    ABOVE("above");

    fun displayString(): String = when (this) {
        BELOW -> "poniżej"
        ABOVE -> "powyżej"
    }
}

/**
 * Model domeny — wynik skanowania AR.
 */
data class ScanResult(
    val detectedText: String,       // Surowy tekst z ML Kit
    val detectedAmount: Double?,    // Rozpoznana kwota
    val detectedCurrency: String?,  // Rozpoznana waluta
    val convertedAmountPln: Double?,// Przeliczona kwota w PLN
    val usedRate: Double?,          // Kurs użyty do przeliczenia
)
