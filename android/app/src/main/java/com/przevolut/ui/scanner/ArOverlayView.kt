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
import android.view.animation.LinearInterpolator
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

    // Scanning line animation — moves vertically within the viewfinder
    private var scanLineProgress = 0f
    private val scanLineAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2500
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animation ->
            scanLineProgress = animation.animatedValue as Float
            invalidate()
        }
    }

    private val density = resources.displayMetrics.density
    private val scaledDensity = resources.displayMetrics.scaledDensity

    private val reticleColor = ContextCompat.getColor(context, R.color.scanner_reticle)
    private val labelBgColor = ContextCompat.getColor(context, R.color.overlay_background)
    private val scrimColor = ContextCompat.getColor(context, R.color.viewfinder_scrim)

    // ── Viewfinder paints ──────────────────────────────────────────────

    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = scrimColor
        style = Paint.Style.FILL
    }

    private val viewfinderBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = reticleColor
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        alpha = 60
    }

    private val viewfinderCornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = reticleColor
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val scanLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    private val hintTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.overlay_text)
        textSize = 14f * scaledDensity
        textAlign = Paint.Align.CENTER
        alpha = 180
    }

    // ── AR label paints (existing) ─────────────────────────────────────

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

    // ── Viewfinder hint text ───────────────────────────────────────────

    private val hintText: String =
        context.getString(R.string.scanner_viewfinder_hint)

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_HARDWARE, null)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (shouldAnimate() && !scanLineAnimator.isRunning) {
            scanLineAnimator.start()
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

        // ── 1. Draw viewfinder reticle ─────────────────────────────────
        drawViewfinder(canvas)

        // ── 2. Draw AR price labels (on top) ───────────────────────────
        drawPriceLabels(canvas)
    }

    /**
     * Draws the viewfinder targeting reticle:
     * - Semi-transparent scrim outside the target area
     * - Thin border rectangle
     * - Thick corner brackets (AR-style)
     * - Animated scanning line
     * - Hint text below
     */
    private fun drawViewfinder(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Viewfinder rectangle: 70% width, aspect ratio ~2:1 (landscape for price tags)
        val vfWidth = w * 0.70f
        val vfHeight = vfWidth * 0.45f
        val vfLeft = (w - vfWidth) / 2f
        val vfTop = (h - vfHeight) / 2f - 30 * density  // slightly above center
        val vfRight = vfLeft + vfWidth
        val vfBottom = vfTop + vfHeight
        val vfRect = RectF(vfLeft, vfTop, vfRight, vfBottom)

        // ── Scrim: darken everything outside the viewfinder ────────────
        // Top strip
        canvas.drawRect(0f, 0f, w, vfTop, scrimPaint)
        // Bottom strip
        canvas.drawRect(0f, vfBottom, w, h, scrimPaint)
        // Left strip
        canvas.drawRect(0f, vfTop, vfLeft, vfBottom, scrimPaint)
        // Right strip
        canvas.drawRect(vfRight, vfTop, w, vfBottom, scrimPaint)

        // ── Thin border ────────────────────────────────────────────────
        canvas.drawRoundRect(vfRect, 8 * density, 8 * density, viewfinderBorderPaint)

        // ── Thick corner brackets ──────────────────────────────────────
        val cornerLen = 28 * density
        drawViewfinderCorners(canvas, vfLeft, vfTop, vfWidth, vfHeight, cornerLen)

        // ── Scanning line ──────────────────────────────────────────────
        if (detectedPrices.isEmpty()) {
            val lineY = vfTop + 8 * density + (vfHeight - 16 * density) * scanLineProgress
            val lineGradient = LinearGradient(
                vfLeft + 16 * density, lineY,
                vfRight - 16 * density, lineY,
                intArrayOf(
                    Color.TRANSPARENT,
                    Color.argb((pulseAlpha * 180).toInt(), 0, 230, 118),
                    Color.argb((pulseAlpha * 180).toInt(), 0, 230, 118),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.2f, 0.8f, 1f),
                Shader.TileMode.CLAMP
            )
            scanLinePaint.shader = lineGradient
            canvas.drawLine(
                vfLeft + 16 * density, lineY,
                vfRight - 16 * density, lineY,
                scanLinePaint
            )
        }

        // ── Hint text below the viewfinder ─────────────────────────────
        if (detectedPrices.isEmpty()) {
            canvas.drawText(
                hintText,
                w / 2f,
                vfBottom + 32 * density,
                hintTextPaint
            )
        }
    }

    /**
     * Draws thick corner brackets around the viewfinder rectangle.
     */
    private fun drawViewfinderCorners(
        canvas: Canvas, x: Float, y: Float,
        w: Float, h: Float, len: Float
    ) {
        // Top-left
        canvas.drawLine(x, y + len, x, y, viewfinderCornerPaint)
        canvas.drawLine(x, y, x + len, y, viewfinderCornerPaint)
        // Top-right
        canvas.drawLine(x + w - len, y, x + w, y, viewfinderCornerPaint)
        canvas.drawLine(x + w, y, x + w, y + len, viewfinderCornerPaint)
        // Bottom-left
        canvas.drawLine(x, y + h - len, x, y + h, viewfinderCornerPaint)
        canvas.drawLine(x, y + h, x + len, y + h, viewfinderCornerPaint)
        // Bottom-right
        canvas.drawLine(x + w - len, y + h, x + w, y + h, viewfinderCornerPaint)
        canvas.drawLine(x + w, y + h, x + w, y + h - len, viewfinderCornerPaint)
    }

    /**
     * Draws AR price labels on top of detected price bounding boxes.
     */
    private fun drawPriceLabels(canvas: Canvas) {
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
        scanLineAnimator.cancel()
    }
}
