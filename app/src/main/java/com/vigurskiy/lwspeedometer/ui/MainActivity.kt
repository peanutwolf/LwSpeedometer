package com.vigurskiy.lwspeedometer.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.vigurskiy.lwspeedometer.R
import com.vigurskiy.lwspeedometer.presenter.MainActivityPresenter
import com.vigurskiy.lwspeedometer.service.DataSourceServiceConnection
import com.vigurskiy.speedometerdatasource.api.DataSourceService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext


class MainActivity :
    AppCompatActivity(),
    CoroutineScope,
    MainActivityPresenter.IndicatorView,
    DashboardViewPager.OnMaxValueChangedListener,
    DataSourceServiceConnection.ConnectionListener
{

    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.Main

    private val logger = LoggerFactory.getLogger(MainActivity::class.java)

    private lateinit var dateSourceConnection: DataSourceServiceConnection

    private lateinit var mainPresenter: MainActivityPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainPresenter = MainActivityPresenter(this).apply {
            start()
        }

        vp_dashboard.onMaxValueChangedListener = this
        vp_dashboard.initDashboard()

        dateSourceConnection = DataSourceServiceConnection(this, this).apply {
            connect()
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                hideNavButtons()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        hideNavButtons()

        mainPresenter.indicatorView = this
    }

    override fun onPause() {
        super.onPause()

        mainPresenter.indicatorView = null
    }

    override fun onDestroy() {
        super.onDestroy()

        vp_dashboard.clearDashboard()
        vp_dashboard.onMaxValueChangedListener = null

        dateSourceConnection.disconnect()

        mainPresenter.stop()

        coroutineContext.cancel()
    }

    override fun onDataSourceConnected(dataSource: DataSourceService) =
        mainPresenter.onDataSourceBound(dataSource)

    override fun onDataSourceConnectionFailed() =
        logger.trace("[onDataSourceConnectionFailed] todo: init reconnection")

    override fun onDataSourceDisconnected() =
        mainPresenter.onDataSourceUnbound()

    override fun onMaxValueChanged(value: Float) =
        mainPresenter.onIndicatorMaxValueChanged(value)

    override fun updateIndicatorValue(value: Float) =
        vp_dashboard.updateIndicatorValue(value)

    private fun hideNavButtons() {
        window.decorView.apply {
            systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

}
