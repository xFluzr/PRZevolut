package com.przevolut.domain.usecase

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConvertCurrencyUseCaseTest {

    private lateinit var useCase: ConvertCurrencyUseCase

    @Before
    fun setUp() {
        useCase = ConvertCurrencyUseCase()
    }

    @Test
    fun `convert EUR to PLN with standard rate`() {
        assertEquals(432.0, useCase(100.0, 4.32), 0.001)
    }

    @Test
    fun `convert zero amount returns zero`() {
        assertEquals(0.0, useCase(0.0, 4.32), 0.001)
    }

    @Test
    fun `convert with fractional amount and rate`() {
        val result = useCase(19.99, 4.3245)
        assertEquals(86.4467, result, 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative amount throws exception`() {
        useCase(-10.0, 4.32)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero rate throws exception`() {
        useCase(100.0, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative rate throws exception`() {
        useCase(100.0, -4.32)
    }

    @Test
    fun `convert GBP with high precision rate`() {
        assertEquals(5.1234, useCase(1.0, 5.1234), 0.0001)
    }

    @Test
    fun `convert large amount`() {
        assertEquals(3_980_000.0, useCase(1_000_000.0, 3.98), 0.01)
    }

    @Test
    fun `convert with very small amount`() {
        assertEquals(0.0432, useCase(0.01, 4.32), 0.0001)
    }
}
