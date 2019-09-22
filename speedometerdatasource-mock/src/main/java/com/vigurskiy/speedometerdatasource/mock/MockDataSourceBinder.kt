package com.vigurskiy.speedometerdatasource.mock

import com.vigurskiy.speedometerdatasource.api.DataSourceService
import com.vigurskiy.speedometerdatasource.api.OnDataChangeListener
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class MockDataSourceBinder(
    private val parentCoroutineScope: CoroutineScope
) : DataSourceService.Stub(){

    private val logger = LoggerFactory.getLogger(MockDataSourceBinder::class.java)

    private val jobChangeMutex = Mutex()

    private var generationJob: Job? = null

    override fun provideData(maxValue: Float, listener: OnDataChangeListener?) = runBlocking{

        jobChangeMutex.withLock {
            generationJob?.cancelAndJoin()

            if(listener != null)
                generationJob = launchGeneration(maxValue, listener)
        }

    }

    private fun launchGeneration(maxValue: Float, listener: OnDataChangeListener): Job{
        return parentCoroutineScope.launch(Dispatchers.IO) {

            logger.debug("[launchGeneration] Start launch generation: maxValue=[{}]", maxValue)

            var sinFunctionValue = SIN_FUNCTION_MIN_VALUE

            do {
                delay(10)
                sinFunctionValue += 0.01f

//                logger.trace("[launchGeneration] value=[{}]", sinFunctionValue)

                if (sinFunctionValue > SIN_FUNCTION_MAX_VALUE)
                    sinFunctionValue = SIN_FUNCTION_MIN_VALUE / 2f

                listener.onDataChange(maxValue * abs(sin(sinFunctionValue)))

            }while (isActive)

            logger.trace("[launchGeneration] Complete launch generation")
        }

    }

    private companion object{
        private const val SIN_FUNCTION_MIN_VALUE = 0f
        private const val SIN_FUNCTION_MAX_VALUE = PI/2f + 2f * PI
    }

}