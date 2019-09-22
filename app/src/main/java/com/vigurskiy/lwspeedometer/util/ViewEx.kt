@file:JvmName("ViewEx")
package com.vigurskiy.lwspeedometer.util

import android.view.View
import androidx.core.view.isVisible

internal fun View.xorVisibility(anotherView: View){
    if (!isVisible.xor(anotherView.isVisible)){
        isVisible = true
        anotherView.isVisible = false
    }else{
        invertGoneVisibility()
        anotherView.invertGoneVisibility()
    }
}

internal fun View.invertGoneVisibility() =
    when(isVisible){
        true -> visibility = View.GONE
        else -> visibility = View.VISIBLE
    }
