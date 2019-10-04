package com.vigurskiy.speedometerdatasource.external

import com.vigurskiy.speedometerdatasource.api.DataSourceService
import com.vigurskiy.speedometerdatasource.api.OnDataChangeListener
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory


class ExternalDataSourceBinder(
    private val parentCoroutineScope: CoroutineScope
) : DataSourceService.Stub() {

    private val logger = LoggerFactory.getLogger(ExternalDataSourceBinder::class.java)

    private val jobChangeMutex = Mutex()

    private var generationJob: Job? = null

    private val generator = SequenceGenerator()

    override fun provideData(maxValue: Float, listener: OnDataChangeListener?) = runBlocking {
        jobChangeMutex.withLock {
            generationJob?.cancelAndJoin()

            if (listener != null)
                generationJob = launchGeneration(maxValue, listener)
        }

    }

    private fun launchGeneration(maxValue: Float, listener: OnDataChangeListener): Job {
        return parentCoroutineScope.launch {
            generator.setMaxValue(maxValue)

            logger.debug("[launchGeneration] Start launch generation: maxValue=[{}]", maxValue)

            do {

                val value = generator.getNextValue()

                if (value < 0)
                    continue

                listener.onDataChange(value)

                delay(GENERATION_DELAY)

            } while (isActive)

            logger.trace("[launchGeneration] Complete launch generation")
        }

    }

    companion object {
        private const val GENERATION_DELAY = 100L
    }

}