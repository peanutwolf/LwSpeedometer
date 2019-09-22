package com.vigurskiy.lwspeedometer

import android.content.Context
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.math.abs

abstract class TwoFingerGestureDetector {

    protected var onFlingListerner: OnFlingListerner? = null
        private set

    fun setOnFlingListener(listener: OnFlingListerner?) {
        onFlingListerner = listener
    }

    interface OnFlingListerner {
        fun onFling(flingDirection: Int)
    }

    companion object {
        const val ON_FLING_UP = 0
        const val ON_FLING_DOWN = 1
        const val ON_FLING_LEFT = 2
        const val ON_FLING_RIGHT = 3
    }
}

class TwoFingerGestureDetectorImpl(
    applicationContext: Context
) : TwoFingerGestureDetector(),
    GestureDetector.OnGestureListener by GestureDetector.SimpleOnGestureListener() {

    private val logger = LoggerFactory.getLogger(TwoFingerGestureDetectorImpl::class.java)

    private val detector = GestureDetectorCompat(applicationContext, this)

    override fun onDown(e: MotionEvent?) = true

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {

        logger.debug("[onFling] Fling detected:\ne1=[{}]\ne2=[{}]", e1, e2)

        if(e1 == null || e2 == null)
            return false

        if (e1.action != ACTION_DOWN && e2.action != ACTION_UP)
            return false

        val dx = e1.x - e2.x
        val dy = e1.y - e2.y

        val dxAbs = abs(dx)
        val dyAbs = abs(dy)

        val direction = if(dxAbs > dyAbs){
            if(dx > 0) ON_FLING_LEFT
            else ON_FLING_RIGHT
        }else{
            if(dy > 0) ON_FLING_UP
            else ON_FLING_DOWN
        }

        onFlingListerner?.onFling(direction)

        return true
    }

    fun onTouchEvent(event: MotionEvent?) = event?.takeIf {
        logger.debug("onTouchEvent: {}", event)

      it.pointerCount == 2
    }?.let {
        detector.onTouchEvent(event)
    }
}

class MainActivity : AppCompatActivity(),
    GestureDetector.OnGestureListener by GestureDetector.SimpleOnGestureListener() {

    private val logger = LoggerFactory.getLogger(MainActivity::class.java)

    private lateinit var gestureDetector: TwoFingerGestureDetectorImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn_refresh.setOnClickListener {
            lw_speedometer.invalidate()
        }

        gestureDetector = TwoFingerGestureDetectorImpl(applicationContext)

        gestureDetector.setOnFlingListener(object : TwoFingerGestureDetector.OnFlingListerner{
            override fun onFling(flingDirection: Int) {
                logger.debug("[onFling] Fling detected: direction=[{}]", flingDirection)

                val view = findViewById<ViewGroup>(R.id.main_layout)
                val fade = Slide()
                TransitionManager.beginDelayedTransition(view, fade)

                if (lw_speedometer.visibility == View.GONE)
                    lw_speedometer.visibility = View.VISIBLE
                else if (lw_speedometer.visibility == View.VISIBLE)
                    lw_speedometer.visibility = View.GONE
                if (lw_tachometer.visibility == View.GONE)
                    lw_tachometer.visibility = View.VISIBLE
                else if (lw_tachometer.visibility == View.VISIBLE)
                    lw_tachometer.visibility = View.GONE

            }
        })

    }

    override fun onStart() {
        super.onStart()

        GlobalScope.launch {
            while (true) {

                var speed = 0f

                do {
                    delay(5)

                    speed += 0.1f

                    withContext(Dispatchers.Main) {
                        //                        lw_speedometer.currentValue = speed
//                        lw_tachometer.currentValue = speed
                    }
                } while (speed < 200f)
            }
        }
    }


    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {


        return true
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        gestureDetector.onTouchEvent(event)


//        if (event?.pointerCount!! > 1) {
//            logger.debug("[onTouchEvent] Pointer count ${event.pointerCount}")
//        }

        return super.onTouchEvent(event)
    }
}
