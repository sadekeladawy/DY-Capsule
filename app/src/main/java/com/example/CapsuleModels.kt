package com.example

import android.graphics.Bitmap
import android.app.PendingIntent

enum class CapsuleState {
    IDLE,
    MEDIA_PLAYING,
    NOTIFICATION_POPUP,
    EXPANDED_MEDIA,
    EXPANDED_NOTIFICATION,
    CHARGING_EVENT
}

data class MediaInfo(
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val albumArt: Bitmap? = null,
    val dominantColor: Int? = null,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val packageName: String? = null
)

data class NotificationInfo(
    val title: String = "",
    val content: String = "",
    val appIcon: Bitmap? = null,
    val largeIcon: Bitmap? = null,
    val contentIntent: PendingIntent? = null
)

data class BatteryInfo(
    val isCharging: Boolean = false,
    val percentage: Int = 0
)
