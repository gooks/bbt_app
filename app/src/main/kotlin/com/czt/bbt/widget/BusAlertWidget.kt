package com.czt.bbt.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.czt.bbt.R
import com.czt.bbt.service.BusAlertService

class BusAlertWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            appWidgetIds.forEach { id ->
                remove("widget_${id}_id")
                remove("widget_${id}_type")
                remove("widget_${id}_title")
            }
            apply()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == "com.czt.bbt.ACTION_WIDGET_REFRESH" || 
            action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            action == Intent.ACTION_BOOT_COMPLETED) {
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BusAlertWidget::class.java)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) 
                ?: appWidgetManager.getAppWidgetIds(componentName)
                
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val widgetPrefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val alertId = widgetPrefs.getLong("widget_${appWidgetId}_id", -1L)
            val type = widgetPrefs.getString("widget_${appWidgetId}_type", "") ?: ""
            val title = widgetPrefs.getString("widget_${appWidgetId}_title", "알림 미지정") ?: "알림 미지정"

            if (alertId == -1L) return

            val views = RemoteViews(context.packageName, R.layout.widget_bus_alert)
            views.setTextViewText(R.id.widget_title, title)

            // 앱 실행 상태 확인
            val appPrefs = context.getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE)
            val isRunning = if (type == "RIDE") {
                appPrefs.getLong("active_ride_id", -1L) == alertId
            } else {
                val arrivalIds = appPrefs.getString("active_arrival_ids", "") ?: ""
                arrivalIds.split(",").contains(alertId.toString())
            }

            // 인텐트 구성
            val serviceIntent = Intent(context, BusAlertService::class.java).apply {
                if (isRunning) {
                    action = if (type == "RIDE") BusAlertService.ACTION_STOP_RIDE else BusAlertService.ACTION_STOP_ALERT
                } else {
                    action = if (type == "RIDE") BusAlertService.ACTION_START_RIDE else BusAlertService.ACTION_START_ARRIVAL
                }
                putExtra(BusAlertService.EXTRA_ALERT_ID, alertId)
            }

            val pendingIntent = PendingIntent.getService(
                context, 
                appWidgetId + 2000, 
                serviceIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 상태에 따른 버튼 색상 및 텍스트 토글
            if (isRunning) {
                // 작동 중 -> 중지 버튼 (빨간색) 보이기
                views.setViewVisibility(R.id.btn_widget_start, View.GONE)
                views.setViewVisibility(R.id.btn_widget_stop, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.btn_widget_stop, pendingIntent)
            } else {
                // 중지됨 -> 시작 버튼 (파란색) 보이기
                views.setViewVisibility(R.id.btn_widget_start, View.VISIBLE)
                views.setViewVisibility(R.id.btn_widget_stop, View.GONE)
                views.setOnClickPendingIntent(R.id.btn_widget_start, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
