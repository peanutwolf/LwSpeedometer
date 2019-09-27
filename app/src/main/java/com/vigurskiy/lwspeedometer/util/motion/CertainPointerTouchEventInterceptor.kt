package com.vigurskiy.lwspeedometer.util.motion

import android.view.MotionEvent

class CertainPointerTouchEventInterceptor(
    private val moveWithPointerCount: Int
) {
    private var movePointerCounter = 0
    private var otherPointerCounter = 0

    fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        when (ev?.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                movePointerCounter = 0
                otherPointerCounter = 0
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (ev.pointerCount == moveWithPointerCount) movePointerCounter++
                else otherPointerCounter++

                if (otherPointerCounter > movePointerCounter)
                    return false
            }
            MotionEvent.ACTION_UP -> {
                if (otherPointerCounter > movePointerCounter)
                    ev.action = MotionEvent.ACTION_CANCEL
            }
            MotionEvent.ACTION_CANCEL ->{
                movePointerCounter = 0
                otherPointerCounter = 0
            }
        }
        return true
    }
}