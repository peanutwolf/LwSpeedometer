@file:JvmName("FloatEx")
package com.vigurskiy.lwspeedometer.util

import kotlin.math.PI

private const val piF = PI.toFloat() //paF

internal fun Float.degreeToRadian(): Float =
    times(piF).div(180f)
