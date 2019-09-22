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


class DataSourceServiceConnection(private val context: Context) : ServiceConnection{

    private val logger =
        LoggerFactory.getLogger(DataSourceServiceConnection::class.java)

    private val connectionMutex = Mutex()

    private val serviceReference = AtomicReference<DataSourceService?>()

    private lateinit var connectionCompletable: CompletableDeferred<DataSourceService>

    suspend fun connect(): DataSourceService? {
        connectionMutex.withLock {
            val serviceConnection = serviceReference.get()

            if (serviceConnection != null)
                return serviceConnection

            connectionCompletable = CompletableDeferred()

            if (tryBindExternalService()){
                logger.trace("[connect] Binding external DataSourceService success")
                return connectionCompletable.await()
            }

            logger.trace("[connect] Failed to bind external DataSourceService, falling back to local")

            if (tryBindLocalService()){
                logger.trace("[connect] Binding local DataSourceService success")
                return connectionCompletable.await()
            }

            logger.trace("[connect] Failed to bind local DataSourceService either, sorry")
            return null
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

    private fun tryBindExternalService(): Boolean{
        val resolvedInfo = context.packageManager.resolveService(Intent(
            EXTERNAL_DATA_SOURCE_ACTION
        ), 0)

        if(resolvedInfo == null){
            logger.debug(
                "[tryBindExternalService] Missing external service: action=[{}]",
                EXTERNAL_DATA_SOURCE_ACTION
                )
            return false
        }

        val intent = Intent().apply {
            action = EXTERNAL_DATA_SOURCE_ACTION
            `package` = resolvedInfo.serviceInfo.packageName
        }

        return context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    private fun tryBindLocalService(): Boolean{
            val intent = Intent(
                context,
                MockDataSourceService::class.java
            ).also {
                it.action = MockDataSourceService.MOCK_DATA_SERVICE_NAME
            }

        return context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    companion object{
        private const val EXTERNAL_DATA_SOURCE_ACTION = "com.vigurskiy.datasourceservice.SERVICE"
    }

}
