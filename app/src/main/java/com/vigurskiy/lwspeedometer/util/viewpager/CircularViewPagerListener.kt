package com.vigurskiy.lwspeedometer.util.viewpager

import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

class CircularViewPagerListener(private val viewPager: ViewPager) : ViewPager.OnPageChangeListener {

    var onPageSelectedListener: ((Int) -> Unit)? = null

    private var currentPosition: Int = 1
    private var previousPosition: Int = -1

    override fun onPageSelected(position: Int) {
        currentPosition = position

        val lastIndex = viewPager.adapter?.lastIndex() ?: return

        val pageSelected = when {

            position == 0 -> lastIndex - 2

            position == lastIndex -> 0

            previousPosition != lastIndex && previousPosition != 0 -> position - 1

            else -> -1
        }

        if (pageSelected >= 0) {
            onPageSelectedListener?.invoke(pageSelected)
        }

        previousPosition = position

    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state != ViewPager.SCROLL_STATE_IDLE)
            return

        val lastIndex = viewPager.adapter?.lastIndex() ?: return

        if (currentPosition == 0){
            viewPager.setCurrentItem(lastIndex - 1, false)
        }

        else if (currentPosition == lastIndex){
            viewPager.setCurrentItem(1, false)
        }

    }

    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) {
    }

    private fun PagerAdapter.lastIndex(): Int = count.minus(1)
}