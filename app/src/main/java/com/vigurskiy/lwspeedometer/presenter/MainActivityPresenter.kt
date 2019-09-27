package com.vigurskiy.lwspeedometer.presenter

import com.vigurskiy.speedometerdatasource.api.DataSourceService
import com.vigurskiy.speedometerdatasource.api.OnDataChangeListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class MainActivityPresenter(private val parentScope: CoroutineScope) : Presenter {

    var indicatorView: IndicatorView? = null

    private var currentMaxValue = 0f
    private var dataSourceService: DataSourceService? = null
    private var dataChangeListener: OnDataChangeListenerImpl? = null

    override fun start() {}

    override fun stop() {
        dataChangeListener?.indicatorViewRef?.set(null)
        dataSourceService?.provideData(0f, null)
    }

    fun onDataSourceBound(dataSource: DataSourceService) {
        dataSourceService = dataSource
        dataChangeListener = OnDataChangeListenerImpl(AtomicReference(indicatorView))
        dataSourceService?.provideData(currentMaxValue, dataChangeListener)
    }

    fun onDataSourceUnbound() {
        dataSourceService = null
    }

    fun onIndicatorMaxValueChanged(indicatorMaxValue: Float) {
        currentMaxValue = indicatorMaxValue

        dataChangeListener?.indicatorViewRef?.set(null)
        dataChangeListener = OnDataChangeListenerImpl(AtomicReference(indicatorView))

        dataSourceService?.provideData(currentMaxValue, dataChangeListener)
    }


    private inner class OnDataChangeListenerImpl(
        val indicatorViewRef: AtomicReference<IndicatorView?>
    ): OnDataChangeListener.Stub() {

        override fun onDataChange(data: Float) {
            parentScope.launch {
                indicatorViewRef.get()?.updateIndicatorValue(data)
            }
        }
    }

    interface IndicatorView {

        fun updateIndicatorValue(value: Float)

    }


}