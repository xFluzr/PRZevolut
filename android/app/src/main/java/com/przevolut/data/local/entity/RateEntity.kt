package com.przevolut.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Encja Room — lokalny cache kursów walut.
 * Umożliwia działanie offline skanera AR.
 */
@Entity(tableName = "exchange_rates")
data class RateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val currency: String,       // "EUR", "USD", "GBP", "CHF", "CZK"
    val rate: Double,
    val mid: Double,
    val effectiveDate: String,
    val fetchedAt: Long,        // System.currentTimeMillis()
)
