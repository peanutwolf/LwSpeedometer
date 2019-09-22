package com.vigurskiy.lwspeedometer.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import org.slf4j.LoggerFactory
import kotlin.math.abs

class TwoFingerGestureDetectorImpl(
    applicationContext: Context
) : TwoFingerGestureDetector(),
    GestureDetector.OnGestureListener by GestureDetector.SimpleOnGestureListener() {

    private val logger = LoggerFactory.getLogger(TwoFingerGestureDetectorImpl::class.java)

    private val detector = GestureDetectorCompat(applicationContext, this)
    private val twoFingerFlingSequence = TwoFingerFlingSequence()

    override fun onDown(e: MotionEvent?) = true

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {

        if (e1 == null || e2 == null)
            return false

        if (!twoFingerFlingSequence.isSequenceDetected())
            return false

        val dx = e1.x - e2.x
        val dy = e1.y - e2.y

        val dxAbs = abs(dx)
        val dyAbs = abs(dy)

        val direction = if (dxAbs > dyAbs) {
            if (dx > 0) ON_FLING_LEFT
            else ON_FLING_RIGHT
        } else {
            if (dy > 0) ON_FLING_UP
            else ON_FLING_DOWN
        }

        onFlingListener?.onFling(direction)

        return true
    }

    fun onTouchEvent(event: MotionEvent?) = event?.let {
        twoFingerFlingSequence.onNextEvent(event)
        detector.onTouchEvent(event)
    }

    private class TwoFingerFlingSequence {
        private var state: String = GESTURE_STATE_UNKNOWN
        private var sequenceIndex = 0

        private val logger by lazy { LoggerFactory.getLogger(TwoFingerFlingSequence::class.java) }

        private val gestureSequence = arrayOf(
            ActionDownStateProcessor,
            PointerDownOrAnyExceptActionUpStateProcessor,
            ActionMoveStateProcessor,
            AnyOrActionUpStateProcessor
        )

        fun isSequenceDetected() = state == GESTURE_STATE_DETECTED

        //Assume that fling is gesture between
        // ACTION_DOWN ->
        // ACTION_POINTER_DOWN(1) or AnyExcept ACTION_UP->
        // nACTION_MOVE->
        // ACTION_POINTER_UP(0,1) ->
        // Any or ACTION_UP ->
        fun onNextEvent(event: MotionEvent) {



            gestureSequence[sequenceIndex].whatNext(event)
                .also { nextState ->

                    logger.debug("[onNextEvent] e=[{}], nextState=[{}]", event, nextState)

                    state = nextState

                    when (nextState) {
                        GESTURE_STATE_MOVE_NEXT -> {
                            sequenceIndex++
                        }
                        GESTURE_STATE_REPEAT -> {
                            //nop
                        }
                        GESTURE_STATE_INTERRUPT -> {
                            sequenceIndex = 0
                        }
                        GESTURE_STATE_DETECTED -> {
                            sequenceIndex = 0
                        }
                    }

                    if (sequenceIndex >= gestureSequence.size) {
                        logger.warn("[onNextEvent] Something went wrong, sequence out of bounds: event=[{}], state=[{}]", event, state)
                        sequenceIndex = 0
                        state = GESTURE_STATE_UNKNOWN
                    }
                }
        }

        private interface GestureStateProcessor {
            fun whatNext(event: MotionEvent): String
        }

        private object ActionDownStateProcessor : GestureStateProcessor {
            override fun whatNext(event: MotionEvent): String = when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> GESTURE_STATE_MOVE_NEXT
                else -> GESTURE_STATE_INTERRUPT
            }
        }

        private object PointerDownOrAnyExceptActionUpStateProcessor : GestureStateProcessor {
            override fun whatNext(event: MotionEvent): String = when {
                event.actionMasked == MotionEvent.ACTION_POINTER_DOWN && event.actionIndex == 1 -> GESTURE_STATE_MOVE_NEXT
                event.actionMasked == MotionEvent.ACTION_UP -> GESTURE_STATE_INTERRUPT
                else -> GESTURE_STATE_REPEAT
            }
        }

        private object ActionMoveStateProcessor : GestureStateProcessor {
            override fun whatNext(event: MotionEvent): String = when {
                event.actionMasked == MotionEvent.ACTION_MOVE -> GESTURE_STATE_REPEAT
                event.actionMasked == MotionEvent.ACTION_POINTER_UP &&
                        (event.actionIndex == 0 || event.actionIndex == 1) -> GESTURE_STATE_MOVE_NEXT
                else -> GESTURE_STATE_REPEAT
            }
        }

        private object AnyOrActionUpStateProcessor : GestureStateProcessor {
            override fun whatNext(event: MotionEvent): String = when (event.actionMasked) {
                MotionEvent.ACTION_UP -> GESTURE_STATE_DETECTED
                else -> GESTURE_STATE_REPEAT
            }
        }

        private companion object {
            private const val GESTURE_STATE_UNKNOWN = "UNKNOWN"
            private const val GESTURE_STATE_MOVE_NEXT = "MOVE_NEXT"
            private const val GESTURE_STATE_REPEAT = "REPEAT"
            private const val GESTURE_STATE_INTERRUPT = "INTERRUPT"
            private const val GESTURE_STATE_DETECTED = "DETECTED"
        }

    }

}