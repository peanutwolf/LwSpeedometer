package com.vigurskiy.speedometerdatasource.external

import com.vigurskiy.speedometerdatasource.api.DataSourceService
import com.vigurskiy.speedometerdatasource.api.OnDataChangeListener
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.math.PI
import kotlin.math.sin


class ExternalDataSourceBinder(
    private val parentCoroutineScope: CoroutineScope
) : DataSourceService.Stub() {

    private val logger = LoggerFactory.getLogger(ExternalDataSourceBinder::class.java)

    private val jobChangeMutex = Mutex()

    private var generationJob: Job? = null

    override fun provideData(maxValue: Float, listener: OnDataChangeListener?) = runBlocking {
        jobChangeMutex.withLock {
            generationJob?.cancelAndJoin()

            if (listener != null)
                generationJob = launchGeneration(maxValue, listener)
        }

    }

    private fun launchGeneration(maxValue: Float, listener: OnDataChangeListener): Job {
        return parentCoroutineScope.launch {

            logger.debug("[launchGeneration] Start launch generation: maxValue=[{}]", maxValue)

            var radianX = 0f

            do {

                radianX += GENERATION_STEP
                val sinFunctionValue = (sin(2 * PI * radianX / GENERATION_SIN_KOEFF1) +
                            sin(PI * radianX / GENERATION_SIN_KOEFF2)) / 2

                val result = maxValue * sinFunctionValue.toFloat()

                if (result < 0)
                    continue

                listener.onDataChange(result)

                delay(GENERATION_DELAY)

            } while (isActive)

            logger.trace("[launchGeneration] Complete launch generation")
        }

    }

    companion object {
        private const val GENERATION_STEP = 20f
        private const val GENERATION_DELAY = 600L
        private const val GENERATION_SIN_KOEFF1 = 500
        private const val GENERATION_SIN_KOEFF2 = 300
    }

}