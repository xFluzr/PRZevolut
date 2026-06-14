package com.przevolut

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.przevolut.data.local.dao.RateDao
import com.przevolut.data.local.entity.RateEntity
import com.przevolut.ui.scanner.ScannerViewModel
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

/**
 * Testy jednostkowe ScannerViewModel.
 * Weryfikuje przeliczanie kwot i obsługę braku kursu w DB.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @MockK
    lateinit var rateDao: RateDao

    private lateinit var viewModel: ScannerViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        every { rateDao.getLatestRates() } returns flowOf(emptyList())

        viewModel = ScannerViewModel(rateDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `processOcrResult with valid EUR text updates scanResult`() = runTest(testDispatcher) {
        val eurRate = RateEntity(
            currency = "EUR", rate = 4.25, mid = 4.25,
            effectiveDate = "2026-05-04", fetchedAt = System.currentTimeMillis()
        )
        coEvery { rateDao.getLatestRate("EUR") } returns eurRate

        viewModel.processOcrResult("€ 12,99")
        advanceTimeBy(500)
        advanceUntilIdle()

        val result = viewModel.scanResult.first()
        assertNotNull(result)
        assertEquals("EUR", result!!.detectedCurrency)
        assertEquals(12.99, result.detectedAmount!!, 0.001)
        assertEquals(12.99 * 4.25, result.convertedAmountPln!!, 0.01)
    }

    @Test
    fun `processOcrResult with no rate in DB returns null convertedAmount`() = runTest(testDispatcher) {
        coEvery { rateDao.getLatestRate(any()) } returns null

        viewModel.processOcrResult("€ 10,00")
        advanceTimeBy(500)
        advanceUntilIdle()

        val result = viewModel.scanResult.first()
        assertNotNull(result)
        assertNull(result!!.convertedAmountPln)
    }

    @Test
    fun `processOcrResult with unrecognizable text leaves scanResult null`() = runTest(testDispatcher) {
        viewModel.processOcrResult("Lorem ipsum dolor")
        advanceTimeBy(500)
        advanceUntilIdle()

        val result = viewModel.scanResult.first()
        assertNull(result)
    }
}
