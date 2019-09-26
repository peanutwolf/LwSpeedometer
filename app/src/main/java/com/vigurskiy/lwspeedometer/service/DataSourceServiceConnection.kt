package com.vigurskiy.lwspeedometer.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.vigurskiy.speedometerdatasource.api.DataSourceService
import com.vigurskiy.speedometerdatasource.external.ExternalDataSourceService
import com.vigurskiy.speedometerdatasource.mock.MockDataSourceService
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger


class DataSourceServiceConnection(
    private val context: Context,
    private val connectionListener: ConnectionListener
) : ServiceConnection {

    private val logger = LoggerFactory.getLogger(DataSourceServiceConnection::class.java)

    private val connectionState = AtomicInteger()

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        logger.debug("[onServiceConnected] DataSourceService connected: name=[{}]", name)

        require(connectionState.compareAndSet(STATE_AWAIT_CONNECTION, STATE_CONNECTED)){
            "Illegal connection state=[${connectionState.get()}]"
        }

        connectionListener.onDataSourceConnected(
            DataSourceService.Stub.asInterface(service)
        )
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        logger.debug("[onServiceDisconnected] DataSourceService disconnected: name=[{}]", name)
        disconnect()
    }

    override fun onBindingDied(name: ComponentName?) {
        logger.debug("[onBindingDied] DataSourceService is dead: name=[{}]", name)
        disconnect()

    }

    override fun onNullBinding(name: ComponentName?) {
        logger.debug("[onNullBinding] DataSourceService returned null: name=[{}]", name)
        disconnect()
    }

    fun connect(): Boolean {

        require(connectionState.compareAndSet(STATE_IDLE, STATE_INIT_CONNECTION)) {
            "Service connection state is not idle, call disconnect heretofore: state=[${connectionState.get()}]"
        }

        if (tryBindExternalService()) {
            logger.trace("[connect] Binding external DataSourceService success")
            connectionState.compareAndSet(STATE_INIT_CONNECTION, STATE_AWAIT_CONNECTION)
            return true
        }

        logger.trace("[connect] Failed to bind external DataSourceService, falling back to local")

        if (tryBindLocalService()) {
            logger.trace("[connect] Binding local DataSourceService success")
            connectionState.compareAndSet(STATE_INIT_CONNECTION, STATE_AWAIT_CONNECTION)
            return true
        }

        logger.trace("[connect] Failed to bind local DataSourceService either, sorry")
        connectionState.compareAndSet(STATE_INIT_CONNECTION, STATE_IDLE)
        return false

    }

    fun disconnect() {
        logger.debug("[disconnect] Disconnecting DataSourceService")

        require(connectionState.get() != STATE_INIT_CONNECTION){
            "Can't disconnect before connection initialisation complete"
        }

        if (connectionState.compareAndSet(STATE_CONNECTED, STATE_IDLE)){
            context.unbindService(this)
            connectionListener.onDataSourceDisconnected()
        }else if(connectionState.compareAndSet(STATE_AWAIT_CONNECTION, STATE_IDLE)){
            context.unbindService(this)
            connectionListener.onDataSourceConnectionFailed()
        }
    }

    private fun tryBindExternalService(): Boolean {
        val resolvedInfo = context.packageManager.resolveService(
            Intent(
                ExternalDataSourceService.EXTERNAL_DATA_SERVICE_NAME
            ), 0
        )

        if (resolvedInfo == null) {
            logger.debug(
                "[tryBindExternalService] Missing external service: action=[{}]",
                ExternalDataSourceService.EXTERNAL_DATA_SERVICE_NAME
            )
            return false
        }

        val intent = Intent().apply {
            action = ExternalDataSourceService.EXTERNAL_DATA_SERVICE_NAME
            `package` = resolvedInfo.serviceInfo.packageName
        }

        return context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    private fun tryBindLocalService(): Boolean {
        val intent = Intent(
            context,
            MockDataSourceService::class.java
        ).also {
            it.action = MockDataSourceService.MOCK_DATA_SERVICE_NAME
        }

        return context.bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    interface ConnectionListener {

        fun onDataSourceConnected(dataSource: DataSourceService)

        fun onDataSourceConnectionFailed()

        fun onDataSourceDisconnected()

    }

    companion object {
        private const val STATE_IDLE = 0
        private const val STATE_INIT_CONNECTION = 1
        private const val STATE_AWAIT_CONNECTION = 2
        private const val STATE_CONNECTED = 3
    }

}
