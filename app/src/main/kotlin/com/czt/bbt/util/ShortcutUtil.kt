package com.czt.bbt.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.czt.bbt.R
import com.czt.bbt.model.ArrivalAlert
import com.czt.bbt.model.RideAlert
import com.czt.bbt.receiver.BusAlertRoutineExecutionActivity

object ShortcutUtil {
    fun updateShortcuts(context: Context, rideAlerts: List<RideAlert>, arrivalAlerts: List<ArrivalAlert>) {
        val shortcuts = mutableListOf<ShortcutInfoCompat>()
        val maxShortcutsPerType = 2 // 타입별 최대 숏컷 수 (전체 4개)

        // 이동 알림 동적 숏컷 추가
        rideAlerts.take(maxShortcutsPerType).forEach { alert ->
            val intent = Intent(context, BusAlertRoutineExecutionActivity::class.java).apply {
                action = "com.czt.bbt.START_RIDE"
                putExtra("alert_id", alert.id)
            }

            val shortcut = ShortcutInfoCompat.Builder(context, "ride_${alert.id}")
                .setShortLabel("[이동]${alert.busNumber}-${alert.destinationStationName}")
                .setLongLabel("[이동] ${alert.busNumber}번 (${alert.destinationStationName}행)")
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build()
            shortcuts.add(shortcut)
        }

        // 도착 알림 동적 숏컷 추가
        arrivalAlerts.take(maxShortcutsPerType).forEach { alert ->
            val intent = Intent(context, BusAlertRoutineExecutionActivity::class.java).apply {
                action = "com.czt.bbt.START_ARRIVAL"
                putExtra("alert_id", alert.id)
            }

            val shortcut = ShortcutInfoCompat.Builder(context, "arrival_${alert.id}")
                .setShortLabel("[도착]${alert.stationName}")
                .setLongLabel("[도착] ${alert.stationName} (${alert.targetBusNames.joinToString(",")})")
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .build()
            shortcuts.add(shortcut)
        }

        // 기존 동적 숏컷을 모두 제거하고 새 목록으로 설정
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
