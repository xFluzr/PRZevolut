package com.przevolut.ui.scanner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.przevolut.domain.model.DetectedPrice

/**
 * Custom View rysujący nakładkę AR na podgląd kamery.
 *
 * Dla każdej wykrytej ceny rysuje:
 * 1. Animowaną ramkę z narożnikami wokół oryginalnej ceny
 * 2. Etykietę z przeliczoną wartością w PLN nad ceną
 * 3. Trójkątną strzałkę łączącą etykietę z ceną
 */
class ArOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var detectedPrices: List<DetectedPrice> = emptyList()
    private var conversionRates: Map<String, Double> = emptyMap()

    // Animacja pulsowania
    private var pulseAlpha = 1.0f
    private val pulseAnimator = ValueAnimator.ofFloat(0.6f, 1.0f).apply {
        duration = 800
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        interpolator = DecelerateInterpolator()
        addUpdateListener { animation ->
            pulseAlpha = animation.animatedValue as Float
            invalidate()
        }
    }

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    // Paint objects
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6004D40")
        style = Paint.Style.FILL
    }

    private val labelBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f * scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255)
        textSize = 13f * scaledDensity
        textAlign = Paint.Align.CENTER
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E676")
        style = Paint.Style.FILL
    }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun updatePrices(prices: List<DetectedPrice>, rates: Map<String, Double>) {
        detectedPrices = prices
        conversionRates = rates

        if (prices.isNotEmpty() && !pulseAnimator.isRunning) {
            pulseAnimator.start()
        } else if (prices.isEmpty() && pulseAnimator.isRunning) {
            pulseAnimator.cancel()
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (dp in detectedPrices) {
            val rate = conversionRates[dp.currency] ?: continue
            val converted = dp.amount * rate

            // Przelicz koordynaty obraz → widok
            val scaleX = width.toFloat() / dp.imageWidth
            val scaleY = height.toFloat() / dp.imageHeight

            val left = dp.boundingBox.left * scaleX
            val top = dp.boundingBox.top * scaleY
            val right = dp.boundingBox.right * scaleX
            val bottom = dp.boundingBox.bottom * scaleY
            val boxW = right - left
            val boxH = bottom - top

            // 1. Ramka pulsująca
            borderPaint.alpha = (pulseAlpha * 255).toInt()
            val rectF = RectF(
                left - 4 * density, top - 4 * density,
                right + 4 * density, bottom + 4 * density
            )
            canvas.drawRoundRect(rectF, 8 * density, 8 * density, borderPaint)

            // 2. Narożniki AR
            val cornerLen = 20 * density
            cornerPaint.alpha = (pulseAlpha * 255).toInt()
            drawArCorners(
                canvas,
                left - 4 * density, top - 4 * density,
                boxW + 8 * density, boxH + 8 * density,
                cornerLen
            )

            // 3. Etykieta z przeliczoną ceną
            val labelText = String.format("%.2f PLN", converted)
            val labelPadding = 14 * density
            val labelHeight = 42 * density
            val labelWidth = (textPaint.measureText(labelText) + labelPadding * 2)
                .coerceAtLeast(boxW)

            val labelLeft = left + (boxW - labelWidth) / 2
            val labelTop = top - labelHeight - 16 * density

            // Tło
            val labelRect = RectF(
                labelLeft, labelTop,
                labelLeft + labelWidth, labelTop + labelHeight
            )
            canvas.drawRoundRect(labelRect, 8 * density, 8 * density, labelBgPaint)
            canvas.drawRoundRect(labelRect, 8 * density, 8 * density, labelBorderPaint)

            // Strzałka (trójkąt)
            val arrowPath = Path().apply {
                val cx = left + boxW / 2
                moveTo(cx - 6 * density, labelTop + labelHeight)
                lineTo(cx, top - 6 * density)
                lineTo(cx + 6 * density, labelTop + labelHeight)
                close()
            }
            canvas.drawPath(arrowPath, arrowPaint)

            // Tekst przeliczonej ceny
            canvas.drawText(
                labelText,
                labelLeft + labelWidth / 2,
                labelTop + labelHeight / 2 + textPaint.textSize / 3,
                textPaint
            )

            // 4. Oznaczenie oryginalnej ceny pod ramką
            canvas.drawText(
                dp.originalText,
                left + boxW / 2,
                bottom + 18 * density,
                smallTextPaint
            )
        }
    }

    private fun drawArCorners(
        canvas: Canvas, x: Float, y: Float,
        w: Float, h: Float, len: Float
    ) {
        // Top-left
        canvas.drawLine(x, y + len, x, y, cornerPaint)
        canvas.drawLine(x, y, x + len, y, cornerPaint)
        // Top-right
        canvas.drawLine(x + w - len, y, x + w, y, cornerPaint)
        canvas.drawLine(x + w, y, x + w, y + len, cornerPaint)
        // Bottom-left
        canvas.drawLine(x, y + h - len, x, y + h, cornerPaint)
        canvas.drawLine(x, y + h, x + len, y + h, cornerPaint)
        // Bottom-right
        canvas.drawLine(x + w - len, y + h, x + w, y + h, cornerPaint)
        canvas.drawLine(x + w, y + h, x + w, y + h - len, cornerPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
    }
}
