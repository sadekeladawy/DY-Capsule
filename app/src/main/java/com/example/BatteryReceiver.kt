package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager

class BatteryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percentage = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else 0
        
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val isConnectedEvent = action == Intent.ACTION_POWER_CONNECTED
        
        CapsuleStateManager.updateBatteryInfo(
            BatteryInfo(isCharging = isCharging, percentage = percentage),
            connected = isConnectedEvent
        )
    }
}
