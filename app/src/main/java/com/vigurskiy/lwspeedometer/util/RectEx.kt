@file:JvmName("RectEx")
package com.vigurskiy.lwspeedometer.util

import android.graphics.RectF

internal fun RectF.resize(width: Float, height: Float): RectF {
    set(
        left - width,
        top - height,
        right + width,
        bottom + height
    )

    return this
}