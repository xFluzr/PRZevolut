package com.przevolut.domain.model

import android.graphics.RectF

/**
 * Cena wykryta przez OCR z pozycją na obrazie kamery.
 * BoundingBox jest w koordynatach obrazu (imageWidth x imageHeight).
 */
data class DetectedPrice(
    val amount: Double,
    val currency: String,
    val originalText: String,
    val boundingBox: RectF,
    val imageWidth: Int,
    val imageHeight: Int
)
