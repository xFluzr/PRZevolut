package com.przevolut.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.przevolut.data.local.AppDatabase
import com.przevolut.data.local.entity.RateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RateDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: RateDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.rateDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveRates() = runTest {
        val rates = listOf(
            RateEntity(currency = "EUR", rate = 4.32, mid = 4.32,
                       effectiveDate = "2026-06-12", fetchedAt = 1000L),
            RateEntity(currency = "USD", rate = 3.98, mid = 3.98,
                       effectiveDate = "2026-06-12", fetchedAt = 1000L)
        )
        dao.upsertRates(rates)
        val result = dao.getAllRates().first()

        assertEquals(2, result.size)
    }

    @Test
    fun getLatestRateReturnsCorrectCurrency() = runTest {
        dao.upsertRates(listOf(
            RateEntity(currency = "EUR", rate = 4.32, mid = 4.32,
                       effectiveDate = "2026-06-12", fetchedAt = 1000L),
            RateEntity(currency = "USD", rate = 3.98, mid = 3.98,
                       effectiveDate = "2026-06-12", fetchedAt = 1000L),
        ))

        val eur = dao.getLatestRate("EUR")

        assertNotNull(eur)
        assertEquals("EUR", eur?.currency)
        assertEquals(4.32, eur?.mid ?: 0.0, 0.001)
    }

    @Test
    fun deleteOldRatesRemovesExpiredEntries() = runTest {
        dao.upsertRates(listOf(
            RateEntity(currency = "EUR", rate = 4.30, mid = 4.30,
                       effectiveDate = "2026-06-10", fetchedAt = 500L),
            RateEntity(currency = "EUR", rate = 4.32, mid = 4.32,
                       effectiveDate = "2026-06-12", fetchedAt = 1000L),
        ))

        dao.deleteOldRates(750L)

        val result = dao.getAllRates().first()
        assertEquals(1, result.size)
        assertEquals(4.32, result[0].mid, 0.001)
    }

    @Test
    fun emptyDatabaseReturnsEmptyFlow() = runTest {
        val result = dao.getAllRates().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getLatestRateForNonexistentCurrencyReturnsNull() = runTest {
        val result = dao.getLatestRate("XYZ")
        assertNull(result)
    }

    @Test
    fun getLatestRatesReturnsOnePerCurrency() = runTest {
        dao.upsertRates(listOf(
            RateEntity(currency = "EUR", rate = 4.30, mid = 4.30,
                       effectiveDate = "2026-06-10", fetchedAt = 500L),
            RateEntity(currency = "EUR", rate = 4.32, mid = 4.32,
                       effectiveDate = "2026-06-12", fetchedAt = 1000L),
            RateEntity(currency = "USD", rate = 3.98, mid = 3.98,
                       effectiveDate = "2026-06-12", fetchedAt = 1000L),
        ))

        val result = dao.getLatestRates().first()
        assertEquals(2, result.size)
        val eurRate = result.find { it.currency == "EUR" }
        assertEquals(4.32, eurRate?.mid ?: 0.0, 0.001)
    }
}
