package com.czt.bbt.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.czt.bbt.data.BusDatabase
import com.czt.bbt.model.ArrivalAlert
import com.czt.bbt.model.RideAlert
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BusAlertWidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var database: BusDatabase // 직접 주입으로 선회

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        intent.extras?.let {
            appWidgetId = it.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WidgetConfigScreen()
                }
            }
        }
    }

    @Composable
    fun WidgetConfigScreen() {
        val scope = rememberCoroutineScope()
        var rideAlerts by remember { mutableStateOf<List<RideAlert>>(emptyList()) }
        var arrivalAlerts by remember { mutableStateOf<List<ArrivalAlert>>(emptyList()) }

        LaunchedEffect(Unit) {
            rideAlerts = database.busDao().getAllRideAlerts().first()
            arrivalAlerts = database.busDao().getAllArrivalAlerts().first()
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("위젯에 연결할 알림 선택", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { Text("버스 이동 알림", style = MaterialTheme.typography.labelLarge) }
                items(rideAlerts) { alert ->
                    ListItem(
                        headlineContent = { Text("[이동] ${alert.busNumber}번") },
                        supportingContent = { Text("목적지: ${alert.destinationStationName}") },
                        modifier = Modifier.clickable { selectAlert(alert.id, "RIDE", "[이동] ${alert.busNumber}번") }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)); Text("버스 도착 알림", style = MaterialTheme.typography.labelLarge) }
                items(arrivalAlerts) { alert ->
                    ListItem(
                        headlineContent = { Text("[도착] ${alert.stationName}") },
                        supportingContent = { Text("대상 버스: ${alert.targetBusNames.joinToString(", ")}") },
                        modifier = Modifier.clickable { selectAlert(alert.id, "ARRIVAL", "[도착] ${alert.stationName}") }
                    )
                }
            }
        }
    }

    private fun selectAlert(id: Long, type: String, title: String) {
        val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("widget_${appWidgetId}_id", id)
            putString("widget_${appWidgetId}_type", type)
            putString("widget_${appWidgetId}_title", title)
            apply()
        }

        // 시스템에 위젯 설정 완료 및 갱신 요청
        val appWidgetManager = AppWidgetManager.getInstance(this)
        BusAlertWidget.updateAppWidget(this, appWidgetManager, appWidgetId)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue) // RESULT_OK 명시
        finish()
    }
}
