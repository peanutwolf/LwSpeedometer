package com.vigurskiy.lwspeedometer.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.annotation.IntDef
import androidx.annotation.LayoutRes
import androidx.core.view.children
import androidx.viewpager.widget.ViewPager
import com.vigurskiy.lwspeedometer.R
import com.vigurskiy.lwspeedometer.util.motion.ActionMoveTouchEventInterceptor
import com.vigurskiy.lwspeedometer.util.viewpager.CircularViewPagerAdapter
import com.vigurskiy.lwspeedometer.util.viewpager.CircularViewPagerListener
import com.vigurskiy.lwspeedometer.util.viewpager.StackPageTransformer
import com.vigurskiy.lwspeedometer.view.LwRoundedArrowIndicatorView
import com.vigurskiy.lwspeedometer.view.LwSpeedometerView
import com.vigurskiy.lwspeedometer.view.LwTachometerView


class DashboardViewPager
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : ViewPager(context, attrs) {

    var onMaxValueChangedListener: OnMaxValueChangedListener? = null

    private val indicatorPageArray = arrayOf(
        IndicatorPage(INDICATOR_TYPE_SPEEDOMETER, R.layout.page_speedometer),
        IndicatorPage(INDICATOR_TYPE_TACHOMETER, R.layout.page_tachometer)
    )

    private val touchEventInterceptor = ActionMoveTouchEventInterceptor(MOVE_POINTER_COUNT)
    private val pageChangeListener = CircularViewPagerListener(this)
    private val pageAdapter = CircularViewPagerAdapter(
        context,
        indicatorPageArray.map { it.resourceId }.toTypedArray()
    )

    @IndicatorTypeDef
    private var visibleIndicatorType = INDICATOR_TYPE_SPEEDOMETER

    init {
        offscreenPageLimit = OFFSCREEN_PAGE_LIMIT
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean =
        touchEventInterceptor.dispatchTouchEvent(ev) && super.dispatchTouchEvent(ev)

    fun initDashboard() {
        clearDashboard()
        setPageTransformer(false, StackPageTransformer())
        adapter = pageAdapter
        pageChangeListener.onPageSelectedListener = ::onPageSelectedListener
        addOnPageChangeListener(pageChangeListener)
        currentItem = 1
    }

    fun clearDashboard() {
        clearOnPageChangeListeners()
        pageChangeListener.onPageSelectedListener = null
        adapter = null
        setPageTransformer(false, null)
    }

    fun updateIndicatorValue(value: Float) {
        updateIndicatorValue(visibleIndicatorType, value)
    }

    private fun updateIndicatorValue(@IndicatorTypeDef indicatorType: Int, value: Float) {

        val pageTag = when (indicatorType) {
            INDICATOR_TYPE_SPEEDOMETER -> context.getString(R.string.tag_page_speedometer)
            INDICATOR_TYPE_TACHOMETER -> context.getString(R.string.tag_page_tachometer)
            else -> throw IllegalArgumentException("Unknown dashboard view type=[$indicatorType]")
        }

        // Since circular ViewPager implicitly swaps two uttermost pages
        // to avoid flickering we need to change values for all indicators with same types
        children.filter { child -> child.tag == pageTag }
            .map { view ->
                view.findViewWithTag<LwRoundedArrowIndicatorView>(context.getString(R.string.tag_indicator))
            }
            .forEach { indicator ->
                indicator.currentValue = value
            }
    }

    private fun onPageSelectedListener(selectedPage: Int) {
        visibleIndicatorType = indicatorPageArray[selectedPage].type

        val maxValue = when (visibleIndicatorType) {
            INDICATOR_TYPE_SPEEDOMETER -> LwSpeedometerView.SPEEDOMETER_MAX_SPEED
            INDICATOR_TYPE_TACHOMETER -> LwTachometerView.TACHOMETER_MAX_SPIN
            else -> 0
        }

        updateIndicatorValue(INDICATOR_TYPE_SPEEDOMETER, 0f)
        updateIndicatorValue(INDICATOR_TYPE_TACHOMETER, 0f)

        onMaxValueChangedListener?.onMaxValueChanged(maxValue.toFloat())
    }

    interface OnMaxValueChangedListener {

        fun onMaxValueChanged(value: Float)

    }

    private data class IndicatorPage(@IndicatorTypeDef val type: Int, @LayoutRes val resourceId: Int)

    companion object {

        @IntDef(
            INDICATOR_TYPE_SPEEDOMETER,
            INDICATOR_TYPE_TACHOMETER
        )
        @Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
        annotation class IndicatorTypeDef

        private const val INDICATOR_TYPE_SPEEDOMETER = 0
        private const val INDICATOR_TYPE_TACHOMETER = 1

        private const val MOVE_POINTER_COUNT = 2
        private const val OFFSCREEN_PAGE_LIMIT = 2
    }
}


