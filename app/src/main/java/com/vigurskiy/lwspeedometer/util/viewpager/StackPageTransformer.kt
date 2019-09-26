package com.vigurskiy.lwspeedometer.util.viewpager

import android.view.View
import androidx.viewpager.widget.ViewPager

class StackPageTransformer : ViewPager.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        view.apply {
            val pageWidth = width
            when {
                position < -1 -> {
                    alpha = 0f
                }
                position <= 0 -> {
                    alpha = 1f
                    translationX = 0f
                }
                position <= 1 -> {
                    alpha = 1 - position
                    translationX = pageWidth * -position

                }
                else -> {
                    alpha = 0f
                }
            }
        }
    }
}