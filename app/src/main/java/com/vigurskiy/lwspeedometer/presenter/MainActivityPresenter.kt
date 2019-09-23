package com.vigurskiy.lwspeedometer.presenter

import android.os.DeadObjectException
import com.vigurskiy.lwspeedometer.presenter.MainActivityPresenter.MainPresenterCommand.*
import com.vigurskiy.lwspeedometer.service.DataSourceServiceConnection
import com.vigurskiy.speedometerdatasource.api.DataSourceService
import com.vigurskiy.speedometerdatasource.api.OnDataChangeListener
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ActorScope
import kotlinx.coroutines.channels.actor
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class MainActivityPresenter(
    private val dataSourceServiceConnection: DataSourceServiceConnection
) : Presenter, CoroutineScope {

    private val actorExceptionHandler = CoroutineExceptionHandler { _, ex ->
        logger.warn("[actorExceptionHandler] Sorry, something unexpected has happened:\n{}", ex)

        if(ex is DeadObjectException){
            //looks like the hosting process is done
            dataSourceService = null
        }
    }

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.Default + actorExceptionHandler

    var indicatorView: IndicatorView? = null

    @Volatile
    private var indicatorValue: Float = 0f
    private var dispatchingJob: Job? = null

    private val logger = LoggerFactory.getLogger(MainActivityPresenter::class.java)

    private var dataSourceService: DataSourceService? = null

    private val mainPresenterActor = actor(
        capacity = 10,
        block = ::actorBlock
    )


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

    fun onIndicatorMaxValueChanged(indicatorMaxValue: Float) = runBlocking {
        if(!mainPresenterActor.isClosedForSend){
            mainPresenterActor.send(SubscribeDataSource(indicatorMaxValue))
        }
    }

    private suspend fun actorBlock(scope: ActorScope<MainPresenterCommand>){
        for (command in scope) {
            when (command) {

                is ConnectDataSourceService -> {
                    dataSourceService = dataSourceServiceConnection.connect()
                }

                is SubscribeDataSource -> {
                    dataSourceService?.provideData(command.maxValue, OnDataChangeListenerImpl())
                }

                is UnsubscribeDataSource -> {
                    dataSourceService?.provideData(0f, null)
                }

                is DisconnectDataSourceService -> {
                    dataSourceServiceConnection.disconnect()
                    command.completable.complete(Unit)
                }
            }
        }
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

    }
}