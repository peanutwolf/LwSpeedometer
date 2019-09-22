package com.vigurskiy.lwspeedometer.ui

import android.os.Build
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.Slide
import android.transition.TransitionManager
import android.view.Gravity
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.vigurskiy.lwspeedometer.R
import com.vigurskiy.lwspeedometer.gesture.TwoFingerGestureDetector
import com.vigurskiy.lwspeedometer.gesture.TwoFingerGestureDetector.Companion.ON_FLING_DOWN
import com.vigurskiy.lwspeedometer.gesture.TwoFingerGestureDetector.Companion.ON_FLING_LEFT
import com.vigurskiy.lwspeedometer.gesture.TwoFingerGestureDetector.Companion.ON_FLING_RIGHT
import com.vigurskiy.lwspeedometer.gesture.TwoFingerGestureDetector.Companion.ON_FLING_UP
import com.vigurskiy.lwspeedometer.gesture.TwoFingerGestureDetectorImpl
import com.vigurskiy.lwspeedometer.presenter.MainActivityPresenter
import com.vigurskiy.lwspeedometer.service.DataSourceServiceConnection
import com.vigurskiy.lwspeedometer.util.xorVisibility
import com.vigurskiy.lwspeedometer.view.LwSpeedometerView
import com.vigurskiy.lwspeedometer.view.LwTachometerView
import kotlinx.android.synthetic.main.activity_main.*
import org.slf4j.LoggerFactory


class MainActivity : AppCompatActivity(),
    TwoFingerGestureDetector.OnFlingListener,
    MainActivityPresenter.IndicatorView {
    private val logger = LoggerFactory.getLogger(MainActivity::class.java)

    private lateinit var dateSourceConnection: DataSourceServiceConnection

    private lateinit var gestureDetector: TwoFingerGestureDetectorImpl

    private lateinit var mainPresenter: MainActivityPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        savedInstanceState?.apply {
            setVisibleViewType(getInt(VIEW_TYPE_KEY))
        }

        gestureDetector = TwoFingerGestureDetectorImpl(applicationContext)
        gestureDetector.setOnFlingListener(this)

        dateSourceConnection = DataSourceServiceConnection(this)
    }

    override fun onResume() {
        super.onResume()

        mainPresenter = MainActivityPresenter(dateSourceConnection).also {
            it.indicatorView = this
        }

        mainPresenter.start()

        mainPresenter.onIndicatorMaxValueChanged(getViewMaxValue())
    }

    override fun onPause() {
        super.onPause()

        mainPresenter.stop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putInt(VIEW_TYPE_KEY, getVisibleViewType())
        }
        super.onSaveInstanceState(outState)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun updateIndicatorValue(value: Float) = setViewValue(value)

    override fun onFling(flingDirection: Int) {

        fun Int.toSlideEdge(): Int =
            when (this) {
                ON_FLING_UP -> Gravity.TOP
                ON_FLING_DOWN -> Gravity.BOTTOM
                ON_FLING_LEFT -> Gravity.START
                ON_FLING_RIGHT -> Gravity.END
                else -> Gravity.BOTTOM
            }

        val transition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            Slide(flingDirection.toSlideEdge())
        else AutoTransition()

        TransitionManager.beginDelayedTransition(main_layout, transition)

        lw_speedometer.xorVisibility(lw_tachometer)

        mainPresenter.onIndicatorMaxValueChanged(getViewMaxValue())
    }

    private fun getVisibleViewType(): Int =
        when {
            lw_speedometer.isVisible -> SPEEDOMETER_TYPE_KEY
            else -> TACHOMETER_TYPE_KEY
        }

    private fun setVisibleViewType(viewType: Int) =
        when(viewType){
            SPEEDOMETER_TYPE_KEY -> {
                lw_speedometer.isVisible = true
                lw_tachometer.isVisible = false
            }
            else -> {
                lw_speedometer.isVisible = false
                lw_tachometer.isVisible = true
            }
        }

    private fun getViewMaxValue(): Float =
        when(getVisibleViewType()){
            SPEEDOMETER_TYPE_KEY -> LwSpeedometerView.SPEEDOMETER_MAX_SPEED
            else -> LwTachometerView.TACHOMETER_MAX_SPIN
        }.run { toFloat() }

    private fun setViewValue(value: Float) =
        when(getVisibleViewType()){
            SPEEDOMETER_TYPE_KEY -> lw_speedometer.currentValue = value
            else -> lw_tachometer.currentValue = value
        }

    companion object{
        private const val VIEW_TYPE_KEY = "VIEW_TYPE_KEY"

        private const val SPEEDOMETER_TYPE_KEY = 0
        private const val TACHOMETER_TYPE_KEY = 1
    }

}
