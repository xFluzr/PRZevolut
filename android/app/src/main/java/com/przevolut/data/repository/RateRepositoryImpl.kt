package com.przevolut.data.repository

import com.przevolut.data.local.dao.RateDao
import com.przevolut.data.mapper.toDomain
import com.przevolut.data.mapper.toEntity
import com.przevolut.data.remote.ApiService
import com.przevolut.domain.model.ExchangeRate
import com.przevolut.domain.repository.RateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementacja repozytorium — offline-first.
 * Room jako single source of truth, Retrofit jako remote source.
 */
class RateRepositoryImpl @Inject constructor(
    private val dao: RateDao,
    private val api: ApiService
) : RateRepository {

    override fun observeRates(): Flow<List<ExchangeRate>> =
        dao.getLatestRates().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun refreshRates(): Result<Unit> = runCatching {
        val response = api.getRates()
        if (response.isSuccessful) {
            val remoteRates = response.body()?.rates ?: return@runCatching
            val now = System.currentTimeMillis()
            val entities = remoteRates.map { it.toEntity(now) }
            dao.upsertRates(entities)
        } else {
            throw Exception("API error: ${response.code()}")
        }
    }

    override suspend fun getRate(currencyCode: String): ExchangeRate? =
        dao.getLatestRate(currencyCode)?.toDomain()
}
