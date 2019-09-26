package com.vigurskiy.lwspeedometer.util.viewpager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewpager.widget.PagerAdapter

class CircularViewPagerAdapter(
    private val context: Context,
    @LayoutRes val  resourceArray: Array<Int>
) : PagerAdapter() {

    private val circularArray = if (resourceArray.size > 1)
        arrayOf(resourceArray.last(), *resourceArray, resourceArray.first())
    else
        resourceArray

    override fun getCount(): Int = circularArray.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        return LayoutInflater.from(context).let { inflater ->
            val view = inflater.inflate(circularArray[position], container, false) as ViewGroup

            container.addView(view)

            return@let view
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) =
        container.removeView(`object` as View)

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

}