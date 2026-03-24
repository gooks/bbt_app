package com.czt.bbt.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.czt.bbt.service.BusAlertService

class BusAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        // 삼성 루틴에서 전달된 Extra 확인 (long 또는 string)
        val alertId = intent.getLongExtra("alert_id", -1L).takeIf { it != -1L }
            ?: intent.getStringExtra("alert_id")?.toLongOrNull() ?: -1L
            
        if (alertId == -1L) return

        val prefs = context.getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE)

        when (action) {
            "com.czt.bbt.START_RIDE" -> {
                startService(context, BusAlertService.ACTION_START_RIDE, alertId)
            }
            "com.czt.bbt.START_ARRIVAL" -> {
                startService(context, BusAlertService.ACTION_START_ARRIVAL, alertId)
            }
            "com.czt.bbt.STOP_ALERT" -> {
                startService(context, BusAlertService.ACTION_STOP_ALERT, alertId)
            }
        }
    }

    private fun startService(context: Context, action: String, alertId: Long) {
        val serviceIntent = Intent(context, BusAlertService::class.java).apply {
            this.action = action
            putExtra(BusAlertService.EXTRA_ALERT_ID, alertId)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
