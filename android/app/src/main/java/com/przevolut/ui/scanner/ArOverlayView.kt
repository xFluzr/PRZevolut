package com.przevolut.ui.scanner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.przevolut.R
import com.przevolut.domain.model.DetectedPrice

class ArOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var detectedPrices: List<DetectedPrice> = emptyList()
    private var conversionRates: Map<String, Double> = emptyMap()
    private var accessibilitySummary: String = ""

    private var pulseAlpha = 1.0f
    private val pulseAnimator = ValueAnimator.ofFloat(0.4f, 1.0f).apply {
        duration = 1500
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

    private val reticleColor = ContextCompat.getColor(context, R.color.scanner_reticle)
    private val labelBgColor = ContextCompat.getColor(context, R.color.overlay_background)

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = reticleColor
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
    }

    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = reticleColor
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelBgColor
        style = Paint.Style.FILL
    }

    private val labelBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = reticleColor
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.overlay_text)
        textSize = 18f * scaledDensity
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.overlay_text)
        alpha = 200
        textSize = 13f * scaledDensity
        textAlign = Paint.Align.CENTER
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = reticleColor
        style = Paint.Style.FILL
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }
    }

    fun updatePrices(prices: List<DetectedPrice>, rates: Map<String, Double>) {
        detectedPrices = prices
        conversionRates = rates

        accessibilitySummary = prices.mapNotNull { dp ->
            val rate = rates[dp.currency] ?: return@mapNotNull null
            val converted = dp.amount * rate
            "${dp.originalText}: ${"%.2f".format(converted)} PLN"
        }.joinToString(", ")

        if (prices.isNotEmpty() && shouldAnimate() && !pulseAnimator.isRunning) {
            pulseAnimator.start()
        } else if (prices.isEmpty() && pulseAnimator.isRunning) {
            pulseAnimator.cancel()
        }

        invalidate()
        if (accessibilitySummary.isNotBlank()) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (dp in detectedPrices) {
            val rate = conversionRates[dp.currency] ?: continue
            val converted = dp.amount * rate

            val scaleX = width.toFloat() / dp.imageWidth
            val scaleY = height.toFloat() / dp.imageHeight

            val left = dp.boundingBox.left * scaleX
            val top = dp.boundingBox.top * scaleY
            val right = dp.boundingBox.right * scaleX
            val bottom = dp.boundingBox.bottom * scaleY
            val boxW = right - left
            val boxH = bottom - top

            borderPaint.alpha = (pulseAlpha * 255).toInt()
            val rectF = RectF(
                left - 4 * density, top - 4 * density,
                right + 4 * density, bottom + 4 * density
            )
            canvas.drawRoundRect(rectF, 8 * density, 8 * density, borderPaint)

            val cornerLen = 20 * density
            cornerPaint.alpha = (pulseAlpha * 255).toInt()
            drawArCorners(
                canvas,
                left - 4 * density, top - 4 * density,
                boxW + 8 * density, boxH + 8 * density,
                cornerLen
            )

            val labelText = String.format("%.2f PLN", converted)
            val labelPadding = 14 * density
            val labelHeight = 42 * density
            val labelWidth = (textPaint.measureText(labelText) + labelPadding * 2)
                .coerceAtLeast(boxW)

            val labelLeft = left + (boxW - labelWidth) / 2
            val labelTop = top - labelHeight - 16 * density

            val labelRect = RectF(
                labelLeft, labelTop,
                labelLeft + labelWidth, labelTop + labelHeight
            )
            canvas.drawRoundRect(labelRect, 8 * density, 8 * density, labelBgPaint)
            canvas.drawRoundRect(labelRect, 8 * density, 8 * density, labelBorderPaint)

            val arrowPath = Path().apply {
                val cx = left + boxW / 2
                moveTo(cx - 6 * density, labelTop + labelHeight)
                lineTo(cx, top - 6 * density)
                lineTo(cx + 6 * density, labelTop + labelHeight)
                close()
            }
            canvas.drawPath(arrowPath, arrowPaint)

            canvas.drawText(
                labelText,
                labelLeft + labelWidth / 2,
                labelTop + labelHeight / 2 + textPaint.textSize / 3,
                textPaint
            )

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
        canvas.drawLine(x, y + len, x, y, cornerPaint)
        canvas.drawLine(x, y, x + len, y, cornerPaint)
        canvas.drawLine(x + w - len, y, x + w, y, cornerPaint)
        canvas.drawLine(x + w, y, x + w, y + len, cornerPaint)
        canvas.drawLine(x, y + h - len, x, y + h, cornerPaint)
        canvas.drawLine(x, y + h, x + len, y + h, cornerPaint)
        canvas.drawLine(x + w - len, y + h, x + w, y + h, cornerPaint)
        canvas.drawLine(x + w, y + h, x + w, y + h - len, cornerPaint)
    }

    override fun onPopulateAccessibilityEvent(event: AccessibilityEvent) {
        super.onPopulateAccessibilityEvent(event)
        if (accessibilitySummary.isNotBlank()) {
            event.contentDescription = accessibilitySummary
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.contentDescription = accessibilitySummary.ifBlank {
            "Nakładka skanera AR"
        }
        info.className = ArOverlayView::class.java.name
    }

    private fun shouldAnimate(): Boolean {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        return scale > 0f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
    }
}
