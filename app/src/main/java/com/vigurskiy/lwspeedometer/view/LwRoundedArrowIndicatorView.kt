package com.vigurskiy.lwspeedometer.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.res.use
import com.vigurskiy.lwspeedometer.R
import com.vigurskiy.lwspeedometer.util.degreeToRadian
import com.vigurskiy.lwspeedometer.util.resize
import com.vigurskiy.lwspeedometer.view.LwRoundedArrowIndicatorView.ScaleDecorationStrategyCommand.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin


abstract class LwRoundedArrowIndicatorView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var currentValue: Float = 0f
        set(value) {
            arrowAnimator.apply {
                val from = if (animatedFraction < ARROW_ANIMATED_FRACTION_THRESHOLD)
                    currentInterpolatedValue lowPassFilter value
                else
                    currentInterpolatedValue

                cancel()
                setFloatValues(from, value)
                start()
            }
        }

    protected abstract val indicatorArcAngle: Float

    protected abstract val indicatorMaxValue: Int

    protected abstract val indicatorScaleCount: Int

    protected abstract val indicatorLegendsCount: Int

    private val ovalStartAngle: Float get() = indicatorArcAngle
    private val ovalEndAngle: Float get() = -(PERIOD_180_DEGREE + indicatorArcAngle * 2)

    private val scaleStartAngle: Float get() = indicatorArcAngle
    private val scaleEndAngle: Float get() = -(PERIOD_180_DEGREE + indicatorArcAngle)

    private val legendStartAngle: Float get() = -(PERIOD_180_DEGREE + indicatorArcAngle)
    private val legendEndAngle: Float get() = indicatorArcAngle

    private val indicatorMaxValueF by lazy { indicatorMaxValue.toFloat() }

    private val arrowAnimator = ValueAnimator().also {
        it.interpolator = LinearInterpolator()
        it.duration = ARROW_FULL_INTERPOLATION_DURATION
        it.addUpdateListener { animation ->
            currentInterpolatedValue = animation.animatedValue as Float
            invalidate()
        }
    }

    private val ovalPaint: Paint = Paint()
    private val arrowPaint: Paint = Paint()
    private val scaleShortsWhitePaint: Paint = Paint()
    private val scaleShortsRedPaint: Paint = Paint()
    private val scaleLongsWhitePaint: Paint = Paint()
    private val scaleLongsRedPaint: Paint = Paint()
    private val legendPaint: Paint = Paint()

    private var legendPadding: Float = SPEEDOMETER_LEGEND_TEXT_OFFSET
    private var legendTypeface: Typeface = Typeface.MONOSPACE
    private var indicatorArrowColor: Int = INDICATOR_ARROW_COLOR

    private var indicatorViewData: IndicatorViewDrawData? = null

    private var currentInterpolatedValue = 0f

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.LwRoundedArrowIndicator, 0, 0)
            .use { typedArray ->

                indicatorArrowColor = typedArray.getColor(
                    R.styleable.LwRoundedArrowIndicator_arrow_color,
                    INDICATOR_ARROW_COLOR
                )

                legendPadding = typedArray.getDimension(
                    R.styleable.LwRoundedArrowIndicator_legend_padding,
                    SPEEDOMETER_LEGEND_TEXT_OFFSET
                )

                typedArray.getString(R.styleable.LwRoundedArrowIndicator_legend_font)
                    ?.also { fontName ->
                        try {
                            legendTypeface = Typeface.createFromAsset(context.assets, fontName)
                        } catch (ex: RuntimeException) {
                            // looks like external font is missing
                        }
                    }
            }
    }

    init {
        ovalPaint.color = Color.BLUE
        ovalPaint.strokeWidth = OVAL_STROKE_WIDTH
        ovalPaint.style = Paint.Style.STROKE
        ovalPaint.isAntiAlias = true

        arrowPaint.color = indicatorArrowColor
        arrowPaint.strokeWidth = INDICATOR_STROKE_WIDTH
        arrowPaint.style = Paint.Style.FILL
        arrowPaint.isAntiAlias = true

        scaleShortsWhitePaint.color = Color.WHITE
        scaleShortsWhitePaint.strokeWidth = SCALE_SHORTS_STROKE_WIDTH
        scaleShortsWhitePaint.style = Paint.Style.STROKE
        scaleShortsWhitePaint.isAntiAlias = true

        scaleShortsRedPaint.color = Color.RED
        scaleShortsRedPaint.strokeWidth = SCALE_SHORTS_STROKE_WIDTH
        scaleShortsRedPaint.style = Paint.Style.STROKE
        scaleShortsRedPaint.isAntiAlias = true

        scaleLongsWhitePaint.color = Color.WHITE
        scaleLongsWhitePaint.strokeWidth = SCALE_LONGS_STROKE_WIDTH
        scaleLongsWhitePaint.style = Paint.Style.STROKE
        scaleLongsWhitePaint.isAntiAlias = true

        scaleLongsRedPaint.color = Color.RED
        scaleLongsRedPaint.strokeWidth = SCALE_LONGS_STROKE_WIDTH
        scaleLongsRedPaint.style = Paint.Style.STROKE
        scaleLongsRedPaint.isAntiAlias = true

        legendPaint.textSize = LEGEND_TEXT_SIZE
        legendPaint.typeface = legendTypeface
        legendPaint.setShadowLayer(LEGEND_PAINT_SHADOW_RADIUS, 0f, 0f, Color.RED)
        legendPaint.color = Color.WHITE
        legendPaint.isAntiAlias = true
    }

    protected abstract fun treatScale(scaleIndex: Int): ScaleDecorationStrategyCommand

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val widthSizeF = widthSize.toFloat()
        val heightSizeF = heightSize.toFloat()

        val drawArea = createDrawAreaRectF(
            widthSizeF,
            heightSizeF,
            indicatorArcAngle
        )

        val drawAreaWidth = drawArea.width().toInt()
        val drawAreaHeight = drawArea.height().toInt()

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(drawAreaWidth, widthSize)
            else -> drawAreaWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(drawAreaHeight, heightSize)
            else -> drawAreaHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        indicatorViewData = createIndicatorView(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val dataToDraw = indicatorViewData ?: return

        canvas.apply {
            drawPath(dataToDraw.scalePath.scaleShortsColoredPath, scaleShortsRedPaint)
            drawPath(dataToDraw.scalePath.scaleShortsWhitePath, scaleShortsWhitePaint)
            drawPath(dataToDraw.scalePath.scaleLongsColoredPath, scaleLongsRedPaint)
            drawPath(dataToDraw.scalePath.scaleLongsWhitePath, scaleLongsWhitePaint)

            drawPath(dataToDraw.ovalPath, ovalPaint)

            dataToDraw.legendData.forEach {
                drawText(it.text, it.xPos, it.yPos, legendPaint)
            }

            save()
            rotate(
                currentInterpolatedValue.valueToAngle(),
                dataToDraw.drawArea.centerX(),
                dataToDraw.drawArea.centerY()
            )
            drawPath(dataToDraw.arrowPath, arrowPaint)
            restore()
        }

    }

    private fun createIndicatorView(width: Int, height: Int): IndicatorViewDrawData {
        val drawArea = createDrawAreaRectF(width.toFloat(), height.toFloat(), indicatorArcAngle)

        val ovalRect = createOvalRect(drawArea)
        val ovalPath = createOvalPath(ovalRect)

        val scaleRect = createScaleRect(ovalRect)
        val scalePath = createScalePath(scaleRect)

        val legendData = precomputeLegendPosition(scaleRect)

        val arrowPath = createArrowPath(drawArea)

        return IndicatorViewDrawData(
            drawArea,
            ovalPath,
            scalePath,
            legendData,
            arrowPath
        )
    }

    private fun calcArcBias(rectSide: Float, viewWidth: Float, arcDegree: Float): Float {
        val hypotenuse = rectSide / 2

        val cathetus = hypotenuse * cos((PERIOD_90_DEGREE - arcDegree).degreeToRadian())
        val bias = hypotenuse - cathetus

        //In case we can grow in width
        return (viewWidth - rectSide).let { dWidth ->
            if (dWidth > 0) min(dWidth, bias)
            else 0f
        }
    }

    private fun createDrawAreaRectF(viewWidth: Float, viewHeight: Float, arcAngle: Float): RectF {
        val viewCenter = PointF(viewWidth / 2, viewHeight / 2)
        val squareSide = min(viewWidth, viewHeight)

        val arcBias = calcArcBias(squareSide, viewWidth, arcAngle)

        val arcRectSide = squareSide + arcBias
        val rectHalfSide = arcRectSide / 2

        val xStart = viewCenter.x - rectHalfSide
        val yStart = viewCenter.y - rectHalfSide + arcBias / 2

        val xEnd = viewCenter.x + rectHalfSide
        val yEnd = viewCenter.y + rectHalfSide + arcBias / 2

        return RectF(xStart, yStart, xEnd, yEnd)
    }

    private fun createOvalRect(drawArea: RectF): RectF =
        RectF(drawArea).apply {
            resize(-OVAL_STROKE_WIDTH / 2, -OVAL_STROKE_WIDTH / 2)
        }

    private fun createOvalPath(ovalRect: RectF): Path =
        Path().apply {
            addArc(ovalRect, ovalStartAngle, ovalEndAngle)
        }

    private fun createScaleRect(ovalRect: RectF): ScaleRectHolder =
        ScaleRectHolder(
            RectF(ovalRect).resize(
                -OVAL_STROKE_WIDTH,
                -OVAL_STROKE_WIDTH
            ),
            RectF(ovalRect).resize(
                -(OVAL_STROKE_WIDTH + SCALE_LONGS_STROKE_WIDTH_QUARTER),
                -(OVAL_STROKE_WIDTH + SCALE_LONGS_STROKE_WIDTH_QUARTER)
            )
        )

    private fun createScalePath(scaleRectHolder: ScaleRectHolder): ScalePathData {
        val scaleLongsColoredPath = Path()
        val scaleLongsWhitePath = Path()
        val scaleShortsColoredPath = Path()
        val scaleShortsWhitePath = Path()

        val step = (abs(scaleStartAngle) + abs(scaleEndAngle)) / indicatorScaleCount

        var startAngleCounter = scaleStartAngle
        for (i in 0..indicatorScaleCount) {
            val startAngle = startAngleCounter - INDICATOR_SCALE_WIDTH / 2

            treatScale(i).also { decoration ->
                when (decoration) {
                    is LongColoredScale -> {
                        scaleLongsColoredPath.addArc(
                            scaleRectHolder.longsRect, startAngle,
                            INDICATOR_SCALE_WIDTH
                        )
                    }

                    LongWhiteScale -> {
                        scaleLongsWhitePath.addArc(
                            scaleRectHolder.longsRect, startAngle,
                            INDICATOR_SCALE_WIDTH
                        )
                    }

                    ShortColoredScale -> {
                        scaleShortsColoredPath.addArc(
                            scaleRectHolder.shortsRect, startAngle,
                            INDICATOR_SCALE_WIDTH
                        )
                    }

                    ShortWhiteScale -> {
                        scaleShortsWhitePath.addArc(
                            scaleRectHolder.shortsRect, startAngle,
                            INDICATOR_SCALE_WIDTH
                        )
                    }
                }
            }

            startAngleCounter -= step
        }

        return ScalePathData(
            scaleLongsColoredPath,
            scaleLongsWhitePath,
            scaleShortsColoredPath,
            scaleShortsWhitePath
        )
    }

    private fun precomputeLegendPosition(scaleRectHolder: ScaleRectHolder): Array<LegendPositionData> {
        val textBounds = Rect()
        val array = mutableListOf<LegendPositionData>()

        fun xOffset(radius: Float, angleRadian: Float) = radius * cos(angleRadian)
        fun yOffset(radius: Float, angleRadian: Float) = radius * sin(angleRadian)

        val legendRect = RectF(scaleRectHolder.longsRect)
        val radius = legendRect.width() / 2 - legendPadding
        val centerX = legendRect.centerX()
        val centerY = legendRect.centerY()

        val step = (abs(legendStartAngle) + abs(legendEndAngle)) / indicatorLegendsCount

        val legendStep = indicatorMaxValue / indicatorLegendsCount

        var startAngleCounter = legendStartAngle
        for (i in 0..indicatorLegendsCount) {

            val speedLegend = "${i * legendStep}"

            legendPaint.getTextBounds(speedLegend, 0, speedLegend.length, textBounds)

            val xBias = xOffset(radius, startAngleCounter.degreeToRadian())
            val yBias = yOffset(radius, startAngleCounter.degreeToRadian())

            array.add(
                LegendPositionData(
                    speedLegend,
                    centerX + xBias - textBounds.width() / 2,
                    centerY + yBias
                )
            )
            startAngleCounter += step
        }

        return array.toTypedArray()
    }

    private fun createArrowPath(drawArea: RectF): Path =
        with(Path()) {
            val centerX = drawArea.centerX()
            val centerY = drawArea.centerY()

            val radius = drawArea.width() / 2
            val arrowBias = radius * ARROW_BIAS_COEFFICIENT

            moveTo(centerX - ARROW_WIDTH, centerY + arrowBias)
            lineTo(
                centerX,
                drawArea.centerY() - radius + OVAL_STROKE_WIDTH + SCALE_SHORTS_STROKE_WIDTH
            )
            lineTo(centerX + ARROW_WIDTH, centerY + arrowBias)

            return@with this
        }

    private fun Float.valueToAngle(): Float {
        val value = when {
            this < 0 -> 0f
            this > indicatorMaxValue -> indicatorMaxValueF
            else -> this
        }

        val step = (abs(scaleStartAngle) + abs(scaleEndAngle)) / indicatorMaxValue

        return -PERIOD_90_DEGREE - indicatorArcAngle + step * value

    }

    private infix fun Float.lowPassFilter(newValue: Float): Float =
        this + (newValue - this)/ ARROW_LOWPASS_FILTER_COEFFICIENT

    protected sealed class ScaleDecorationStrategyCommand {
        object ShortWhiteScale : ScaleDecorationStrategyCommand()
        object ShortColoredScale : ScaleDecorationStrategyCommand()
        object LongWhiteScale : ScaleDecorationStrategyCommand()
        object LongColoredScale : ScaleDecorationStrategyCommand()
    }

    private data class IndicatorViewDrawData(
        val drawArea: RectF,
        val ovalPath: Path,
        val scalePath: ScalePathData,
        val legendData: Array<LegendPositionData>,
        val arrowPath: Path
    )

    private data class ScaleRectHolder(
        val shortsRect: RectF,
        val longsRect: RectF
    )

    private data class ScalePathData(
        val scaleLongsColoredPath: Path,
        val scaleLongsWhitePath: Path,
        val scaleShortsColoredPath: Path,
        val scaleShortsWhitePath: Path
    )

    private data class LegendPositionData(
        val text: String,
        val xPos: Float,
        val yPos: Float
    )

    companion object {

        private const val INDICATOR_SCALE_WIDTH = 1f

        private const val ARROW_FULL_INTERPOLATION_DURATION = 300L
        private const val ARROW_ANIMATED_FRACTION_THRESHOLD = 0.1f
        private const val ARROW_LOWPASS_FILTER_COEFFICIENT = 50
        private const val ARROW_WIDTH = 30f
        private const val ARROW_BIAS_COEFFICIENT = 0.2f

        private const val INDICATOR_ARROW_COLOR = Color.GREEN
        private const val LEGEND_PAINT_SHADOW_RADIUS = 5f
        private const val SPEEDOMETER_LEGEND_TEXT_OFFSET = 50f
        private const val LEGEND_TEXT_SIZE = 50f

        private const val OVAL_STROKE_WIDTH = 25f
        private const val INDICATOR_STROKE_WIDTH = 1f
        private const val SCALE_SHORTS_STROKE_WIDTH = 25f
        private const val SCALE_LONGS_STROKE_WIDTH = 50f
        private const val SCALE_LONGS_STROKE_WIDTH_QUARTER = SCALE_LONGS_STROKE_WIDTH / 4

        private const val PERIOD_180_DEGREE = 180f
        private const val PERIOD_90_DEGREE = 90f
    }
}