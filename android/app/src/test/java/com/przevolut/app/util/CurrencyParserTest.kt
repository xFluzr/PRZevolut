package com.przevolut.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyParserTest {

    @Test
    fun `parsePrice returns correct amount and currency for standard format`() {
        val result = CurrencyParser.parsePrice("19.99 EUR")
        assertEquals(19.99, result?.amount)
        assertEquals("EUR", result?.currencyCode)
    }

    @Test
    fun `parsePrice handles comma as decimal separator`() {
        val result = CurrencyParser.parsePrice("19,99 EUR")
        assertEquals(19.99, result?.amount)
        assertEquals("EUR", result?.currencyCode)
    }

    @Test
    fun `parsePrice handles space as thousand separator`() {
        val result = CurrencyParser.parsePrice("1 999,99 PLN")
        assertEquals(1999.99, result?.amount)
        assertEquals("PLN", result?.currencyCode)
    }

    @Test
    fun `parsePrice handles symbol before price`() {
        val result = CurrencyParser.parsePrice("$4.50")
        assertEquals(4.50, result?.amount)
        assertEquals("USD", result?.currencyCode)
    }

    @Test
    fun `parsePrice uses default currency if not found`() {
        val result = CurrencyParser.parsePrice("4.50", defaultCurrency = "GBP")
        assertEquals(4.50, result?.amount)
        assertEquals("GBP", result?.currencyCode)
    }

    @Test
    fun `parsePrice returns null for invalid text`() {
        val result = CurrencyParser.parsePrice("hello world")
        assertNull(result)
    }
}
