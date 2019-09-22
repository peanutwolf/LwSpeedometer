package com.vigurskiy.lwspeedometer.presenter

import com.vigurskiy.lwspeedometer.presenter.MainActivityPresenter.MainPresenterCommand.*
import com.vigurskiy.lwspeedometer.service.InAppDataSourceServiceConnection
import com.vigurskiy.speedometerdatasource.api.DataSourceService
import com.vigurskiy.speedometerdatasource.api.OnDataChangeListener
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

class MainActivityPresenter(
    private val dataSourceServiceConnection: InAppDataSourceServiceConnection
) : Presenter, CoroutineScope {

    private val actorExceptionHandler = CoroutineExceptionHandler { _, ex ->
        logger.warn("[actorExceptionHandler] Sorry, something unexpected has happened:\n{}", ex)
    }

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Default + actorExceptionHandler

    var indicatorView: IndicatorView? = null

    @Volatile
    private var indicatorValue: Float = 0f
    private var dispatchingJob: Job? = null

    private val logger = LoggerFactory.getLogger(MainActivityPresenter::class.java)

    private var dataSourceService: DataSourceService? = null

    private val mainPresenterActor = actor<MainPresenterCommand>(capacity = 10) {
        for (command in channel) {
            when (command) {
                is ConnectDataSourceService -> connectDataSource()
                is SubscribeDataSource -> subscribeDataSource(command.maxValue)
                is UpdateIndicatorView -> updateIndicatorView(command.withValue)
                is UnsubscribeDataSource -> unsubscribeDataSource()
                is DisconnectDataSourceService -> disconnectDataSource(command.completable)
            }
        }
    }

    override fun start() {
        dispatchingJob = launch {
            mainPresenterActor.send(ConnectDataSourceService)

            loopDispatchingJob()
        }
    }

    override fun stop() {
        runBlocking {

            dispatchingJob?.cancel()

            mainPresenterActor.send(UnsubscribeDataSource)
            with(CompletableDeferred<Unit>()) {
                mainPresenterActor.send(DisconnectDataSourceService(this))
                await()
            }
            mainPresenterActor.close()

            return@runBlocking Unit
        }

        coroutineContext.cancel()
    }

    fun onIndicatorMaxValueUpdate(indicatorMaxValue: Float) = runBlocking {
        mainPresenterActor.send(SubscribeDataSource(indicatorMaxValue))
    }

    private suspend fun connectDataSource() {
        dataSourceService = dataSourceServiceConnection.connect()
    }

    private suspend fun subscribeDataSource(maxValue: Float) {
        dataSourceService?.provideData(maxValue, OnDataChangeListenerImpl())
    }

    private suspend fun updateIndicatorView(withValue: Float) = withContext(Dispatchers.Main) {
        indicatorView?.updateIndicatorValue(withValue)
    }

    private suspend fun unsubscribeDataSource() {
        dataSourceService?.provideData(0f, null)
    }

    private suspend fun disconnectDataSource(completable: CompletableDeferred<Unit>) {
        dataSourceServiceConnection.disconnect()
        completable.complete(Unit)
    }

    private suspend fun CoroutineScope.loopDispatchingJob() {

        var previousValue: Float = indicatorValue

        while (isActive) {
            delay(1)

            val updatedValue = indicatorValue

            if (updatedValue != previousValue) {
                previousValue = updatedValue
                withContext(Dispatchers.Main) {
                    indicatorView?.updateIndicatorValue(updatedValue)
                }
            }
        }
    }


    private inner class OnDataChangeListenerImpl : OnDataChangeListener.Stub() {
        override fun onDataChange(data: Float) {
            indicatorValue = data
        }
    }

    interface IndicatorView {

        fun updateIndicatorValue(value: Float)

    }

    private sealed class MainPresenterCommand {
        object ConnectDataSourceService : MainPresenterCommand()

        class DisconnectDataSourceService(
            val completable: CompletableDeferred<Unit>
        ) : MainPresenterCommand()

        class SubscribeDataSource(val maxValue: Float) : MainPresenterCommand()

        object UnsubscribeDataSource : MainPresenterCommand()

        class UpdateIndicatorView(val withValue: Float) : MainPresenterCommand()

    }
}