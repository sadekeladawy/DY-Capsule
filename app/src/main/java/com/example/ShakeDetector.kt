package com.example

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShake: () -> Unit) : SensorEventListener {

    private var lastShakeTime: Long = 0
    private val SHAKE_THRESHOLD_GRAVITY = 2.7f
    private val SHAKE_SLOP_TIME_MS = 500

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        // Calculate g-force (1.0 is normal resting gravity)
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            val now = System.currentTimeMillis()
            // Ignore shake events that are too close to each other
            if (now - lastShakeTime > SHAKE_SLOP_TIME_MS) {
                lastShakeTime = now
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not required
    }
}
