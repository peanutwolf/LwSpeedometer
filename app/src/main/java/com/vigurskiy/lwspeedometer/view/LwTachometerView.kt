package com.vigurskiy.lwspeedometer.view

import android.content.Context
import android.util.AttributeSet

class LwTachometerView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LwRoundedArrowIndicatorView(context, attrs, defStyleAttr) {

    override val indicatorArcAngle: Float = TACHOMETER_ARC_ANGLE

    override val indicatorMaxValue: Int = TACHOMETER_MAX_SPIN

    override val indicatorScaleCount: Int = TACHOMETER_SCALE_COUNT

    override val indicatorLegendsCount: Int = TACHOMETER_LEGENDS_COUNT

    override fun treatScale(scaleIndex: Int): ScaleDecorationStrategyCommand =
        if (scaleIndex < 7 && scaleIndex % TACHOMETER_SCALE_LONGS_STEP == 0)
            ScaleDecorationStrategyCommand.LongColoredScale
        else if (scaleIndex < 7)
            ScaleDecorationStrategyCommand.ShortColoredScale
        else
            ScaleDecorationStrategyCommand.ShortWhiteScale

    companion object {
        const val TACHOMETER_MAX_SPIN = 80

        // Angle of speedometer's bottom arc part(half of it)
        private const val TACHOMETER_ARC_ANGLE = 10f
        private const val TACHOMETER_SCALE_LONGS_STEP = 3
        private const val TACHOMETER_SCALE_COUNT = 16
        private const val TACHOMETER_LEGENDS_COUNT = 8
    }

}