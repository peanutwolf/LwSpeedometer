package com.vigurskiy.lwspeedometer.view

import android.content.Context
import android.util.AttributeSet

class LwSpeedometerView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LwRoundedArrowIndicatorView(context, attrs, defStyleAttr) {

    override val indicatorArcAngle: Float = SPEEDOMETER_ARC_ANGLE

    override val indicatorMaxValue: Int = SPEEDOMETER_MAX_SPEED

    override val indicatorScaleCount: Int = SPEEDOMETER_MAX_SPEED

    override val indicatorLegendsCount: Int = SPEEDOMETER_LEGEND_COUNT

    override fun treatScale(scaleIndex: Int): ScaleDecorationStrategyCommand =
        if (scaleIndex % SPEEDOMETER_SCALE_LONGS_STEP == 0)
            ScaleDecorationStrategyCommand.LongColoredScale
        else
            ScaleDecorationStrategyCommand.ShortWhiteScale


    companion object {
        const val SPEEDOMETER_MAX_SPEED = 180

        // Angle of speedometer's bottom arc part(half of it)
        private const val SPEEDOMETER_ARC_ANGLE = 45f
        private const val SPEEDOMETER_SCALE_LONGS_STEP = 10 //Mark every 10th scale
        private const val SPEEDOMETER_LEGEND_COUNT = SPEEDOMETER_MAX_SPEED / 20

    }
}