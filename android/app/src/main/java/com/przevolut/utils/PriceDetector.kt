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
        "EUR", "USD", "GBP", "CHF", "CZK", "HUF", "SEK", "NOK", "DKK", "UAH", "RON", "TRY", "ISK"
    )

    private val PRICE_PATTERN = Regex(
        """([€$£]|Fr|Kč)?\s*(\d{1,7}[.,]\d{2})\s*([€$£]|Fr|Kč)?"""
    )
    private val CODE_PATTERN = Regex(
        """(EUR|USD|GBP|CHF|CZK|HUF|SEK|NOK|DKK|UAH|RON|TRY|ISK)\s*(\d{1,7}[.,]\d{2})|(\d{1,7}[.,]\d{2})\s*(EUR|USD|GBP|CHF|CZK|HUF|SEK|NOK|DKK|UAH|RON|TRY|ISK)"""
    )

    data class VirtualLine(
        val elements: MutableList<Text.Element>,
        var boundingBox: RectF
    )

    fun detect(
        visionText: Text,
        imageWidth: Int,
        imageHeight: Int
    ): List<DetectedPrice> {
        val results = mutableListOf<DetectedPrice>()
        val virtualLines = groupIntoVirtualLines(visionText)

        for (vLine in virtualLines) {
            val lineText = reconstructVirtualLineText(vLine)
            val lineBox = vLine.boundingBox

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
                                    lineBox.left,
                                    lineBox.top,
                                    lineBox.right,
                                    lineBox.bottom
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
                                    lineBox.left,
                                    lineBox.top,
                                    lineBox.right,
                                    lineBox.bottom
                                ),
                                imageWidth = imageWidth,
                                imageHeight = imageHeight
                            )
                        )
                    }
                }
            }
        }

        return results.distinctBy { "${it.amount}_${it.currency}" }
    }

    fun reconstructVisionText(visionText: Text): String {
        val sb = java.lang.StringBuilder()
        val virtualLines = groupIntoVirtualLines(visionText)
        for (vLine in virtualLines) {
            sb.append(reconstructVirtualLineText(vLine)).append("\n")
        }
        return sb.toString()
    }

    private fun groupIntoVirtualLines(visionText: Text): List<VirtualLine> {
        val allElements = mutableListOf<Text.Element>()
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                allElements.addAll(line.elements)
            }
        }
        
        allElements.sortWith(compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 }))
        val virtualLines = mutableListOf<VirtualLine>()
        
        for (element in allElements) {
            val box = element.boundingBox ?: continue
            val rect = RectF(box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat())
            
            var added = false
            for (vLine in virtualLines) {
                val vBox = vLine.boundingBox
                val verticalOverlap = maxOf(0f, minOf(vBox.bottom, rect.bottom) - maxOf(vBox.top, rect.top))
                val hasVerticalOverlap = verticalOverlap > 0f
                
                // Zapobiegamy łączeniu elementów z bardzo odległych kolumn
                val horizontalGap = rect.left - vBox.right
                val isHorizontallyClose = horizontalGap < (vBox.bottom - vBox.top) * 3f
                
                if (hasVerticalOverlap && isHorizontallyClose) {
                    vLine.elements.add(element)
                    vLine.boundingBox.left = minOf(vBox.left, rect.left)
                    vLine.boundingBox.top = minOf(vBox.top, rect.top)
                    vLine.boundingBox.right = maxOf(vBox.right, rect.right)
                    vLine.boundingBox.bottom = maxOf(vBox.bottom, rect.bottom)
                    added = true
                    break
                }
            }
            if (!added) {
                virtualLines.add(VirtualLine(mutableListOf(element), rect))
            }
        }
        
        for (vLine in virtualLines) {
            vLine.elements.sortBy { it.boundingBox?.left ?: 0 }
        }
        return virtualLines
    }

    /**
     * Sprawdza czy element curr jest superscriptem wzgl. prev na podstawie:
     * 1. Różnicy wysokości (curr mniejszy o >= 15%)
     * 2. Pozycji pionowej (dół curr jest powyżej 70% wysokości prev)
     */
    private fun isSuperscript(
        prevBox: android.graphics.Rect, currBox: android.graphics.Rect
    ): Boolean {
        val prevHeight = prevBox.bottom - prevBox.top
        val currHeight = currBox.bottom - currBox.top
        val isSmallerHeight = prevHeight > 1.15f * currHeight
        val isPositionedHigher = currBox.bottom < prevBox.top + prevHeight * 0.75f
        return isSmallerHeight || isPositionedHigher
    }

    private fun reconstructVirtualLineText(vLine: VirtualLine): String {
        var text = vLine.elements.joinToString(" ") { it.text }
        var decimalInserted = false
        
        // 1) Symbol-level: szukaj superscriptu wewnątrz pojedynczego elementu
        //    np. ML Kit scalił "3" i "93" w jedno słowo "393"
        for (element in vLine.elements) {
            val symbols = element.symbols
            for (i in 0 until symbols.size - 1) {
                val prev = symbols[i]
                val curr = symbols[i + 1]
                val prevBox = prev.boundingBox
                val currBox = curr.boundingBox
                
                if (prevBox != null && currBox != null) {
                    val prevIsDigit = prev.text.firstOrNull()?.isDigit() == true
                    val currIsDigit = curr.text.firstOrNull()?.isDigit() == true
                    
                    if (prevIsDigit && currIsDigit && isSuperscript(prevBox, currBox)) {
                        val originalElementText = element.text
                        if (originalElementText.length == symbols.size) {
                            val newElementText = originalElementText.substring(0, i + 1) + "." + originalElementText.substring(i + 1)
                            text = text.replaceFirst(originalElementText, newElementText)
                            decimalInserted = true
                        }
                        break
                    }
                }
            }
        }

        // 2) Element-level: szukaj superscriptu pomiędzy oddzielnymi elementami
        //    np. ML Kit rozdzielił na "6" i "98"
        val elements = vLine.elements
        for (i in 0 until elements.size - 1) {
            val prev = elements[i]
            val curr = elements[i + 1]
            val prevBox = prev.boundingBox
            val currBox = curr.boundingBox
            
            if (prevBox != null && currBox != null) {
                val prevEndsWithDigit = prev.text.lastOrNull()?.isDigit() == true
                val currStartsWithDigit = curr.text.firstOrNull()?.isDigit() == true

                if (prevEndsWithDigit && currStartsWithDigit && isSuperscript(prevBox, currBox)) {
                    val withoutSpace = "${prev.text}${curr.text}"
                    val withSpace = "${prev.text} ${curr.text}"
                    val replacement = "${prev.text}.${curr.text}"
                    
                    if (text.contains(withSpace)) {
                        text = text.replaceFirst(withSpace, replacement)
                        decimalInserted = true
                    } else if (text.contains(withoutSpace)) {
                        text = text.replaceFirst(withoutSpace, replacement)
                        decimalInserted = true
                    }
                }
            }
        }

        // 3) Fallback: jeśli bounding boxy nie zadziałały, ale mamy
        //    symbol waluty + 3+ cyfr BEZ separatora dziesiętnego →
        //    wstaw kropkę przed ostatnimi 2 cyframi.
        //    Np. "$393" → "$3.93", "$1488" → "$14.88"
        //    W retailu ceny ZAWSZE mają grosze, więc to bezpieczna heurystyka.
        if (!decimalInserted) {
            text = SUPERSCRIPT_FALLBACK_BEFORE.replace(text) { m ->
                "${m.groupValues[1]}${m.groupValues[2]}.${m.groupValues[3]}"
            }
            text = SUPERSCRIPT_FALLBACK_AFTER.replace(text) { m ->
                "${m.groupValues[1]}.${m.groupValues[2]}${m.groupValues[3]}"
            }
        }

        return text
    }

    // Symbol waluty + 3+ cyfr bez kropki → wstaw kropkę przed ostatnimi 2 cyframi
    private val SUPERSCRIPT_FALLBACK_BEFORE = Regex(
        """([€$£]|Fr|Kč)\s*(\d+)(\d{2})(?!\d|[.,]\d)"""
    )
    private val SUPERSCRIPT_FALLBACK_AFTER = Regex(
        """(?<!\d[.,])(\d+)(\d{2})\s*([€$£]|Fr|Kč)"""
    )
}
