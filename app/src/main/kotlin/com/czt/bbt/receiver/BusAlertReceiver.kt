package com.czt.bbt.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.czt.bbt.service.BusAlertService

class BusAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        // 루틴에서 보낸 Extra 데이터 확인 (long 또는 string 대응)
        val alertId = intent.getLongExtra("alert_id", -1L).takeIf { it != -1L }
            ?: intent.getStringExtra("alert_id")?.toLongOrNull() ?: -1L
            
        if (alertId == -1L) return

        val serviceIntent = Intent(context, BusAlertService::class.java).apply {
            putExtra(BusAlertService.EXTRA_ALERT_ID, alertId)
        }

        when (action) {
            "com.czt.bbt.ACTION_BUS_RIDE_START", "com.czt.bbt.START_RIDE" -> {
                serviceIntent.action = BusAlertService.ACTION_START_RIDE
            }
            "com.czt.bbt.ACTION_BUS_ARRIVAL_START", "com.czt.bbt.START_ARRIVAL" -> {
                serviceIntent.action = BusAlertService.ACTION_START_ARRIVAL
            }
            "com.czt.bbt.ACTION_BUS_STOP", "com.czt.bbt.STOP_ALERT" -> {
                serviceIntent.action = BusAlertService.ACTION_STOP_ALERT
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
