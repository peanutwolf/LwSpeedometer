package com.vigurskiy.lwspeedometer

import android.app.Application
import com.codemonkeylabs.fpslibrary.TinyDancer

class LwSpeedometerApp : Application(){


    override fun onCreate() {
        super.onCreate()

        TinyDancer.create()
            .show(applicationContext)


//        TinyDancer.create()
//            .redFlagPercentage(.1f)
//            .startingXPosition(200)
//            .startingYPosition(600)
//            .show(applicationContext)
    }
}