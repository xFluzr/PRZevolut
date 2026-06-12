package com.przevolut.data.mapper

import com.przevolut.data.local.entity.RateEntity
import com.przevolut.data.remote.model.RateResponse
import com.przevolut.domain.model.ExchangeRate

/**
 * Mapery między warstwami data ↔ domain.
 */
fun RateEntity.toDomain() = ExchangeRate(
    currency = currency,
    rate = rate,
    mid = mid,
    effectiveDate = effectiveDate,
    fetchedAt = fetchedAt
)

fun RateResponse.toEntity(fetchedAt: Long) = RateEntity(
    currency = currency,
    rate = mid ?: rate ?: 0.0,
    mid = mid ?: rate ?: 0.0,
    effectiveDate = effectiveDate ?: "",
    fetchedAt = fetchedAt
)
