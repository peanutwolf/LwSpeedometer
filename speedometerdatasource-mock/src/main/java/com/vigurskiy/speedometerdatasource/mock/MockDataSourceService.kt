package com.vigurskiy.speedometerdatasource.mock

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

class MockDataSourceService : Service(), CoroutineScope{

    override val coroutineContext: CoroutineContext = SupervisorJob()

    private lateinit var dateSourceBinder: MockDataSourceBinder

    override fun onCreate() {
        super.onCreate()
        dateSourceBinder = MockDataSourceBinder(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return when(intent?.action) {
            MOCK_DATA_SERVICE_NAME -> dateSourceBinder
            else -> null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineContext.cancel()
    }

    companion object{
        const val MOCK_DATA_SERVICE_NAME = "com.vigurskiy.speedometerdatasource.mock.SERVICE"
    }
}