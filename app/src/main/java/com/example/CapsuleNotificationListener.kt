package com.example

import android.app.Notification
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class CapsuleNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val notification = sbn.notification
        val isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0
        if (isOngoing) return 

        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        
        if (title.isEmpty() && text.isEmpty()) return
        
        val largeIcon = notification.getLargeIcon()?.let { convertIconToBitmap(it) }
        val smallIcon = notification.smallIcon?.let { convertIconToBitmap(it) }

        val info = NotificationInfo(
            title = title,
            content = text,
            appIcon = smallIcon,
            largeIcon = largeIcon,
            contentIntent = notification.contentIntent
        )

        CapsuleStateManager.postNotification(info)
    }

    private fun convertIconToBitmap(icon: Icon): Bitmap? {
        return try {
            val drawable = icon.loadDrawable(this) ?: return null
            val bitmap = android.graphics.Bitmap.createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1),
                android.graphics.Bitmap.Config.ARGB_8888
            )
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
