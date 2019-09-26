package com.vigurskiy.speedometerdatasource.external

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

class ExternalDataSourceService : Service(), CoroutineScope {

    override val coroutineContext: CoroutineContext = SupervisorJob()

    private val logger = LoggerFactory.getLogger(ExternalDataSourceService::class.java)

    private lateinit var dateSourceBinder: ExternalDataSourceBinder

    override fun onCreate() {
        super.onCreate()
        logger.trace("[onCreate] Create external data source binder")
        dateSourceBinder = ExternalDataSourceBinder(this)
    }

    override fun onBind(intent: Intent?): IBinder? {

        return when(intent?.action) {
            EXTERNAL_DATA_SERVICE_NAME -> dateSourceBinder
            else -> null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel()
    }

    companion object{
        const val EXTERNAL_DATA_SERVICE_NAME = "com.vigurskiy.speedometerdatasource.external.SERVICE"
    }
}