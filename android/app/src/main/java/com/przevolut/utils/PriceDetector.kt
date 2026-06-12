package com.przevolut.utils

import android.graphics.RectF
import com.przevolut.domain.model.DetectedPrice
import com.google.mlkit.vision.text.Text

/**
 * Analizuje wynik ML Kit i wyodrębnia ceny z pozycjami na obrazie.
 * Wykorzystuje bounding boxy z elementów tekstu do pozycjonowania AR overlay.
 */
object PriceDetector {

    private val CURRENCY_SYMBOLS = mapOf(
        "€" to "EUR", "$" to "USD", "£" to "GBP",
        "Fr" to "CHF", "Kč" to "CZK"
    )
    private val CURRENCY_CODES = setOf(
        "EUR", "USD", "GBP", "CHF", "CZK", "HUF", "SEK", "NOK", "DKK"
    )

    private val PRICE_PATTERN = Regex(
        """([€$£]|Fr|Kč)?\s*(\d{1,7}[.,]\d{2})\s*([€$£]|Fr|Kč)?"""
    )
    private val CODE_PATTERN = Regex(
        """(EUR|USD|GBP|CHF|CZK|HUF|SEK|NOK|DKK)\s*(\d{1,7}[.,]\d{2})|(\d{1,7}[.,]\d{2})\s*(EUR|USD|GBP|CHF|CZK|HUF|SEK|NOK|DKK)"""
    )

    fun detect(
        visionText: Text,
        imageWidth: Int,
        imageHeight: Int
    ): List<DetectedPrice> {
        val results = mutableListOf<DetectedPrice>()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val lineText = line.text
                val lineBox = line.boundingBox ?: continue

                // Szukaj cen z symbolami walut
                PRICE_PATTERN.findAll(lineText).forEach { match ->
                    val symbolBefore = match.groupValues[1]
                    val amountStr = match.groupValues[2]
                    val symbolAfter = match.groupValues[3]

                    val symbol = symbolBefore.ifEmpty { symbolAfter }
                    val currency = CURRENCY_SYMBOLS[symbol]

                    if (currency != null) {
                        val amount = amountStr.replace(",", ".").toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            results.add(
                                DetectedPrice(
                                    amount = amount,
                                    currency = currency,
                                    originalText = match.value.trim(),
                                    boundingBox = RectF(
                                        lineBox.left.toFloat(),
                                        lineBox.top.toFloat(),
                                        lineBox.right.toFloat(),
                                        lineBox.bottom.toFloat()
                                    ),
                                    imageWidth = imageWidth,
                                    imageHeight = imageHeight
                                )
                            )
                        }
                    }
                }

                // Szukaj cen z kodami ISO
                CODE_PATTERN.findAll(lineText).forEach { match ->
                    val code = (match.groupValues[1] + match.groupValues[4]).ifEmpty { null }
                    val amountStr = (match.groupValues[2] + match.groupValues[3]).ifEmpty { null }

                    if (code != null && amountStr != null && code in CURRENCY_CODES) {
                        val amount = amountStr.replace(",", ".").toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            results.add(
                                DetectedPrice(
                                    amount = amount,
                                    currency = code,
                                    originalText = match.value.trim(),
                                    boundingBox = RectF(
                                        lineBox.left.toFloat(),
                                        lineBox.top.toFloat(),
                                        lineBox.right.toFloat(),
                                        lineBox.bottom.toFloat()
                                    ),
                                    imageWidth = imageWidth,
                                    imageHeight = imageHeight
                                )
                            )
                        }
                    }
                }
            }
        }

        return results.distinctBy { "${it.amount}_${it.currency}" }
    }
}
