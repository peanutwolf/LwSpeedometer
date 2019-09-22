package com.vigurskiy.lwspeedometer.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.vigurskiy.speedometerdatasource.api.DataSourceService
import com.vigurskiy.speedometerdatasource.mock.MockDataSourceService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

class InAppDataSourceServiceConnection(
    private val context: Context
) : ServiceConnection {

    private val logger =
        LoggerFactory.getLogger(InAppDataSourceServiceConnection::class.java)

    private val connectionMutex = Mutex()

    private val serviceReference =
        AtomicReference<DataSourceService?>()

    private lateinit var connectionCompletable: CompletableDeferred<DataSourceService>

    suspend fun connect(): DataSourceService? {
        connectionMutex.withLock {
            val serviceConnection = serviceReference.get()

            if (serviceConnection != null)
                return serviceConnection

            connectionCompletable = CompletableDeferred()

            val intent = Intent(
                context,
                MockDataSourceService::class.java
            ).also {
                it.action =
                    MockDataSourceService.MOCK_DATA_SERVICE_NAME
            }

            val res = context.bindService(intent, this, Context.BIND_AUTO_CREATE)

            logger.trace("[connect] Binding DataSourceService: result=[{}]", res)
            if(!res)
                return null

            return connectionCompletable.await()

        }
    }

    suspend fun disconnect() {
        connectionMutex.withLock {
            logger.debug("[disconnect] Disconnecting DataSourceService")
            if (serviceReference.get() != null) {
                context.unbindService(this)
                serviceReference.set(null)
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        logger.debug("[onServiceConnected] DataSourceService connected: name=[{}]", name)

        val binding =
            DataSourceService.Stub.asInterface(service)

        serviceReference.set(binding)
        connectionCompletable.complete(binding)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        logger.debug("[onServiceDisconnected] DataSourceService disconnected: name=[{}]", name)
        serviceReference.set(null)
    }
}