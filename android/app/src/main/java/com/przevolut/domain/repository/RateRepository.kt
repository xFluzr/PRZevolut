package com.przevolut.domain.repository

import com.przevolut.domain.model.ExchangeRate
import kotlinx.coroutines.flow.Flow

/**
 * Interfejs repozytorium kursów walut.
 * Warstwa domain nie zna implementacji (Room, Retrofit).
 */
interface RateRepository {
    /** Obserwuj aktualne kursy z lokalnego cache. */
    fun observeRates(): Flow<List<ExchangeRate>>

    /** Odśwież kursy z serwera. Zwraca Result.success lub Result.failure. */
    suspend fun refreshRates(): Result<Unit>

    /** Pobierz najnowszy kurs dla danej waluty. */
    suspend fun getRate(currencyCode: String): ExchangeRate?
}
