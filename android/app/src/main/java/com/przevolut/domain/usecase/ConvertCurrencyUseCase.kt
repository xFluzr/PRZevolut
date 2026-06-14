package com.przevolut.domain.usecase

import javax.inject.Inject

/**
 * Use case — przeliczanie kwoty z waluty obcej na PLN.
 * Niezależny od frameworków, łatwy do testowania.
 */
class ConvertCurrencyUseCase @Inject constructor() {
    /**
     * Przelicza kwotę na PLN.
     * @param amount kwota w walucie źródłowej (>= 0)
     * @param rateToPln kurs średni NBP (> 0)
     * @return kwota w PLN
     * @throws IllegalArgumentException jeśli argumenty nieprawidłowe
     */
    operator fun invoke(amount: Double, rateToPln: Double): Double {
        require(amount >= 0) { "Kwota nie może być ujemna" }
        require(rateToPln > 0) { "Kurs musi być dodatni" }
        return amount * rateToPln
    }
}
