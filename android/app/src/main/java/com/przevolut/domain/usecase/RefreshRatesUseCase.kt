package com.przevolut.domain.usecase

import com.przevolut.domain.repository.RateRepository
import javax.inject.Inject

/**
 * Use case — odświeżenie kursów z serwera.
 */
class RefreshRatesUseCase @Inject constructor(
    private val repository: RateRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshRates()
}
