package com.przevolut.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.przevolut.data.local.dao.RateDao
import com.przevolut.data.local.entity.RateEntity

/**
 * Główna baza danych Room.
 * Wersja 1 — zawiera tabelę exchange_rates.
 *
 * Dostęp przez Hilt: wstrzykuj RateDao bezpośrednio.
 */
@Database(
    entities = [RateEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rateDao(): RateDao

    companion object {
        const val DATABASE_NAME = "przevolut_db"
    }
}
