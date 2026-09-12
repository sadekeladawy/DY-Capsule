package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class CapsuleOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var lifecycleHelper: OverlayLifecycleHelper? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var stateJob: Job? = null
    
    private val batteryReceiver = BatteryReceiver()
    private var sensorManager: SensorManager? = null
    private var shakeDetector: ShakeDetector? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        
        MediaControllerManager.init(this)

        scope.launch {
            val initialOffset = CapsulePreferences.getYOffset(this@CapsuleOverlayService).first()
            CapsuleStateManager.setYOffset(initialOffset)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(batteryReceiver, filter)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector {
            CapsuleStateManager.resetAll()
        }
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager?.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_UI)
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupOverlayView()

        stateJob = CapsuleStateManager.capsuleYOffset.onEach { offset ->
            updateLayoutParams(offset)
        }.launchIn(scope)
    }

    private fun startForegroundService() {
        val channelId = "capsule_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Material Capsule Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Material Capsule Active")
            .setContentText("Drawing dynamic overlay")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun setupOverlayView() {
        composeView = ComposeView(this).apply {
            setContent {
                CapsuleUI()
            }
        }

        lifecycleHelper = OverlayLifecycleHelper().apply {
            composeView!!.setViewTreeLifecycleOwner(this)
            composeView!!.setViewTreeViewModelStoreOwner(this)
            composeView!!.setViewTreeSavedStateRegistryOwner(this)
            start()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = CapsuleStateManager.capsuleYOffset.value.toInt()
        }

        windowManager.addView(composeView, params)
    }

    private fun updateLayoutParams(yOffset: Float) {
        val view = composeView ?: return
        val params = view.layoutParams as WindowManager.LayoutParams
        params.y = yOffset.toInt()
        windowManager.updateViewLayout(view, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        shakeDetector?.let { sensorManager?.unregisterListener(it) }
        MediaControllerManager.destroy()
        stateJob?.cancel()
        lifecycleHelper?.stop()
        composeView?.let {
            windowManager.removeView(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
