package com.przevolut.domain.usecase

import com.przevolut.domain.model.ExchangeRate
import com.przevolut.domain.repository.RateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case — pobieranie listy kursów walut jako Flow.
 */
class GetRatesUseCase @Inject constructor(
    private val repository: RateRepository
) {
    operator fun invoke(): Flow<List<ExchangeRate>> = repository.observeRates()
}
