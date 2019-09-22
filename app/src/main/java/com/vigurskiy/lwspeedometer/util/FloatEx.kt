@file:JvmName("FloatEx")
package com.vigurskiy.lwspeedometer.util

import kotlin.math.PI

internal fun Float.degreeToRadian(): Float =
    times(PI.toFloat()).div(180f)
