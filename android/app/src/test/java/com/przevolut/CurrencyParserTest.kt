package com.przevolut

import com.przevolut.utils.CurrencyParser
import org.junit.Assert.*
import org.junit.Test

/**
 * Testy jednostkowe parsera walut OCR.
 * Pokrywa różne formaty cen z etykiet sklepowych.
 */
class CurrencyParserTest {

    // ── Testy pozytywne ────────────────────────────────────────────────────

    @Test
    fun `parse euro with symbol before amount`() {
        val result = CurrencyParser.parse("€ 12,99")
        assertNotNull(result)
        assertEquals("EUR", result!!.second)
        assertEquals(12.99, result.first, 0.001)
    }

    @Test
    fun `parse euro without space`() {
        val result = CurrencyParser.parse("€12.99")
        assertNotNull(result)
        assertEquals("EUR", result!!.second)
        assertEquals(12.99, result.first, 0.001)
    }

    @Test
    fun `parse EUR code after amount`() {
        val result = CurrencyParser.parse("49.99 EUR")
        assertNotNull(result)
        assertEquals("EUR", result!!.second)
        assertEquals(49.99, result.first, 0.001)
    }

    @Test
    fun `parse USD symbol`() {
        val result = CurrencyParser.parse("$ 5.50")
        assertNotNull(result)
        assertEquals("USD", result!!.second)
        assertEquals(5.50, result.first, 0.001)
    }

    @Test
    fun `parse GBP pound sign`() {
        val result = CurrencyParser.parse("£ 8.99")
        assertNotNull(result)
        assertEquals("GBP", result!!.second)
        assertEquals(8.99, result.first, 0.001)
    }

    @Test
    fun `parse CHF code`() {
        val result = CurrencyParser.parse("CHF 3,20")
        assertNotNull(result)
        assertEquals("CHF", result!!.second)
        assertEquals(3.20, result.first, 0.001)
    }

    @Test
    fun `parse CZK code`() {
        val result = CurrencyParser.parse("CZK 299")
        assertNotNull(result)
        assertEquals("CZK", result!!.second)
        assertEquals(299.0, result.first, 0.001)
    }

    @Test
    fun `parse amount embedded in longer text`() {
        val result = CurrencyParser.parse("Cena: € 12,99 za sztukę")
        assertNotNull(result)
        assertEquals("EUR", result!!.second)
        assertEquals(12.99, result.first, 0.001)
    }

    // ── Testy negatywne ────────────────────────────────────────────────────

    @Test
    fun `return null for empty string`() {
        val result = CurrencyParser.parse("")
        assertNull(result)
    }

    @Test
    fun `return null for text without currency`() {
        val result = CurrencyParser.parse("Hello world 123")
        assertNull(result)
    }

    @Test
    fun `return null for unsupported currency`() {
        val result = CurrencyParser.parse("100 JPY")
        assertNull(result)
    }

    // ── Testy superscriptu (grosze w indeksie górnym) ────────────────────

    @Test
    fun `parse dollar superscript cents 393 as 3_93`() {
        val result = CurrencyParser.parse("\$393")
        assertNotNull(result)
        assertEquals("USD", result!!.second)
        assertEquals(3.93, result.first, 0.001)
    }

    @Test
    fun `parse dollar superscript cents 1488 as 14_88`() {
        val result = CurrencyParser.parse("\$1488")
        assertNotNull(result)
        assertEquals("USD", result!!.second)
        assertEquals(14.88, result.first, 0.001)
    }

    @Test
    fun `parse euro superscript cents 1499 as 14_99`() {
        val result = CurrencyParser.parse("€1499")
        assertNotNull(result)
        assertEquals("EUR", result!!.second)
        assertEquals(14.99, result.first, 0.001)
    }

    @Test
    fun `parse superscript with space between symbol and number`() {
        val result = CurrencyParser.parse("\$ 698")
        assertNotNull(result)
        assertEquals("USD", result!!.second)
        assertEquals(6.98, result.first, 0.001)
    }

    @Test
    fun `normal decimal takes priority over superscript fallback`() {
        val result = CurrencyParser.parse("\$3.93")
        assertNotNull(result)
        assertEquals("USD", result!!.second)
        assertEquals(3.93, result.first, 0.001)
    }
}
