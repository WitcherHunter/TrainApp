package com.daohang.trainapp

import android.app.Application
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.HandlerThread
import androidx.core.content.getSystemService
import com.daohang.trainapp.constants.DEVICE_1700
import com.daohang.trainapp.constants.NETWORK_STATE_CHANGE
import com.daohang.trainapp.constants.TIME_STATE_CHANGE
import com.daohang.trainapp.livebus.NetworkStates
import com.daohang.trainapp.livebus.TimeStates
import com.daohang.trainapp.services.AMapLocationServices
import com.daohang.trainapp.services.ObdService
import com.daohang.trainapp.utils.SoundManager
import com.daohang.trainapp.utils.WarningFlag
import com.daohang.trainapp.utils.currentTimeWithMinutes
import com.jeremyliao.liveeventbus.LiveEventBus
import com.yz.lz.modulapi.JNIUtils
import com.yz.lz.modulapi.NewJNIUtils
import xcrash.XCrash
import kotlin.concurrent.thread

class MyApplication : Application() {

    companion object {

        lateinit var instance: MyApplication
            private set

        //定位是否已开始
        var locationStarted = false

        var locationEnabled = false

        var networkEnabled = false

        var time = currentTimeWithMinutes()

        var checkNetwork = false

        var checkGps = false

        var DeviceType = DEVICE_1700

        var IMEI = ""

        val ttsHandlerthread = HandlerThread("tts")

        val rfidInstance =
            if (JNIUtils.getInstance().openRfidDevice()) JNIUtils.getInstance() else NewJNIUtils.getInstance()
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        checkNetwork = getSystemService<ConnectivityManager>() != null
        checkGps = getSystemService<LocationManager>() != null

//        initCrashReport()

        WarningFlag.initFlagArray()

        ObdService("ttyS6",57600).open()

        initLocation()

        startMonitorNetworkState()

        startMonitorTime()

        initTTS()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        XCrash.init(this)
    }
//
//    private fun initCrashReport(){
//        LogReport.getInstance()
//            .setCacheSize(30 * 1024 * 1024)
//            .setLogDir(this, "sdcard/" + this.getString(this.applicationInfo.labelRes))
//            .setLogSaver(CrashWriter(this))
//            .init(this)
//    }

//    private fun initEmailReporter() {
//        val email = EmailReporter(this)
//        email.setReceiver("zjz6220659@163.com")
//        email.setSender()
//    }

    private fun initLocation(){
//        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
//        intentFilter.addAction(GEOFENCE_BROADCAST_ACTION)
//        registerReceiver(GeoFenceReceiver(), intentFilter)

        AMapLocationServices(this)
    }

    private fun initTTS(){
        ttsHandlerthread.start()

        SoundManager.initOffline(this)
    }

    /**
     * 刷新当前时间
     */
    private fun startMonitorTime() {
        thread {
            while (true) {
                time = currentTimeWithMinutes()
                val t = TimeStates(time)
                LiveEventBus.get(TIME_STATE_CHANGE)
                    .post(t)
                Thread.sleep(1000)
            }
        }
    }

    /**
     * 监控网络变化
     */
    private fun startMonitorNetworkState() {
        thread {
            while (true) {
                networkEnabled = checkNetworkState()
                val state = NetworkStates(networkEnabled)
                LiveEventBus.get(NETWORK_STATE_CHANGE)
                    .post(state)
                Thread.sleep(5000)
            }
        }
    }

    /**
     * ping百度，监测网络是否可用
     */
    private fun checkNetworkState(): Boolean {
        val command = "ping -c 1 www.baidu.com"

        networkEnabled = try {
            Runtime.getRuntime().exec(command).waitFor()
        } catch (e: Exception) {
            -1
        } == 0

        return networkEnabled
    }

}