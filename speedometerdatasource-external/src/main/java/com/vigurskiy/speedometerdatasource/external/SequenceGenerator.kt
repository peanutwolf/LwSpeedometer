package com.vigurskiy.speedometerdatasource.external

class SequenceGenerator {

    external fun setMaxValue(value: Float)

    external fun getNextValue(): Float

    companion object{
        init {
            System.loadLibrary("SequenceGenerator")
        }
    }
}