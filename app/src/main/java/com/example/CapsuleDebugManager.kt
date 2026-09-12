package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object CapsuleDebugManager {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var mockTickerJob: Job? = null

    fun simulateCharging() {
        CapsuleStateManager.updateBatteryInfo(
            BatteryInfo(isCharging = true, percentage = 85),
            connected = true
        )
    }

    fun simulateNotification() {
        CapsuleStateManager.postNotification(
            NotificationInfo(
                title = "Ahmed",
                content = "Hey, are we still meeting tonight?",
                appIcon = createMockIcon(Color.GREEN, "W"),
                largeIcon = createMockIcon(Color.BLUE, "A")
            )
        )
    }

    fun simulateMusicPlaying() {
        CapsuleStateManager.updateMediaInfo(
            MediaInfo(
                title = "Starboy",
                artist = "The Weeknd",
                isPlaying = true,
                albumArt = createMockIcon(Color.RED, "S"),
                dominantColor = Color.RED,
                duration = 240000L,
                currentPosition = 60000L
            )
        )
        startMockTicker()
    }

    fun simulateMusicPaused() {
        val currentInfo = CapsuleStateManager.mediaInfo.value
        CapsuleStateManager.updateMediaInfo(currentInfo.copy(isPlaying = false))
        stopMockTicker()
    }

    fun simulateIslandSplit() {
        simulateMusicPlaying()
        simulateNotification()
    }

    fun resetToIdle() {
        stopMockTicker()
        CapsuleStateManager.resetAll()
    }
    
    private fun startMockTicker() {
        if (mockTickerJob?.isActive == true) return
        mockTickerJob = scope.launch {
            while (isActive) {
                val currentInfo = CapsuleStateManager.mediaInfo.value
                if (currentInfo.isPlaying && currentInfo.duration > 0) {
                    val nextPosition = (currentInfo.currentPosition + 1000L).coerceAtMost(currentInfo.duration)
                    CapsuleStateManager.updateMediaInfo(currentInfo.copy(currentPosition = nextPosition))
                }
                delay(1000)
            }
        }
    }

    private fun stopMockTicker() {
        mockTickerJob?.cancel()
        mockTickerJob = null
    }

    private fun createMockIcon(color: Int, text: String): Bitmap {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
        }
        canvas.drawCircle(50f, 50f, 50f, paint)
        paint.color = Color.WHITE
        paint.textSize = 50f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, 50f, 65f, paint)
        return bitmap
    }
}
