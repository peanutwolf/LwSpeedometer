package com.vigurskiy.lwspeedometer.gesture

abstract class TwoFingerGestureDetector {

    protected var onFlingListener: OnFlingListener? = null
        private set

    fun setOnFlingListener(listener: OnFlingListener?) {
        onFlingListener = listener
    }

    interface OnFlingListener {
        fun onFling(flingDirection: Int)
    }

    companion object {
        const val ON_FLING_UP = 0
        const val ON_FLING_DOWN = 1
        const val ON_FLING_LEFT = 2
        const val ON_FLING_RIGHT = 3
    }
}