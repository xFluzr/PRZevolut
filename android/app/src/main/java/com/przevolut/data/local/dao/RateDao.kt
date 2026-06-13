package com.przevolut.data.local.dao

import androidx.room.*
import com.przevolut.data.local.entity.RateEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO dla kursów walut — operacje na lokalnym cache.
 */
@Dao
interface RateDao {

    /** Wszystkie kursy jako Flow (automatyczna aktualizacja UI). */
    @Query("SELECT * FROM exchange_rates ORDER BY fetchedAt DESC")
    fun getAllRates(): Flow<List<RateEntity>>

    /** Najnowszy kurs dla danej waluty. */
    @Query("""
        SELECT * FROM exchange_rates 
        WHERE currency = :currency 
        ORDER BY fetchedAt DESC 
        LIMIT 1
    """)
    suspend fun getLatestRate(currency: String): RateEntity?

    /** Najnowszy kurs dla każdej waluty (dla ekranu Dashboard). */
    @Query("""
        SELECT * FROM exchange_rates 
        WHERE fetchedAt = (
            SELECT MAX(fetchedAt) FROM exchange_rates er2 
            WHERE er2.currency = exchange_rates.currency
        )
    """)
    fun getLatestRates(): Flow<List<RateEntity>>

    /** Zapisuje lub nadpisuje kurs (upsert). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRate(rate: RateEntity)

    /** Historia kursów jednej waluty (lokalne odświeżenia). */
    @Query("""
        SELECT * FROM exchange_rates 
        WHERE currency = :currency 
        ORDER BY fetchedAt ASC
    """)
    suspend fun getHistoryForCurrency(currency: String): List<RateEntity>

    /** Zapisuje listę kursów. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRates(rates: List<RateEntity>)

    /** Usuwa stare wpisy (starsze niż X dni). */
    @Query("DELETE FROM exchange_rates WHERE fetchedAt < :olderThan")
    suspend fun deleteOldRates(olderThan: Long)
}
