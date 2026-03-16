package com.czt.bbt.receiver

import android.app.Activity
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
import javax.inject.Inject

@AndroidEntryPoint
class BusAlertRoutineConfigActivity : ComponentActivity() {

    @Inject lateinit var database: BusDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RoutineConfigScreen()
                }
            }
        }
    }

    @Composable
    fun RoutineConfigScreen() {
        var rideAlerts by remember { mutableStateOf<List<RideAlert>>(emptyList()) }
        var arrivalAlerts by remember { mutableStateOf<List<ArrivalAlert>>(emptyList()) }

        LaunchedEffect(Unit) {
            rideAlerts = database.busDao().getAllRideAlerts().first()
            arrivalAlerts = database.busDao().getAllArrivalAlerts().first()
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("루틴 실행 시 동작할 알림 선택", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item { Text("버스 이동 알림", style = MaterialTheme.typography.labelLarge) }
                items(rideAlerts) { alert ->
                    ListItem(
                        headlineContent = { Text("이동-${alert.busNumber}번 시작") },
                        supportingContent = { Text("목적지: ${alert.destinationStationName}") },
                        modifier = Modifier.clickable { selectForRoutine(alert.id, "RIDE") }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)); Text("버스 도착 알림", style = MaterialTheme.typography.labelLarge) }
                items(arrivalAlerts) { alert ->
                    ListItem(
                        headlineContent = { Text("도착-${alert.stationName} 시작") },
                        supportingContent = { Text("대상 버스: ${alert.targetBusNames.joinToString(", ")}") },
                        modifier = Modifier.clickable { selectForRoutine(alert.id, "ARRIVAL") }
                    )
                }
            }
        }
    }

    private fun selectForRoutine(id: Long, type: String) {
        val label = if (type == "RIDE") "[이동] 시작 (ID:$id)" else "[도착] 시작 (ID:$id)"
        
        // 실제 실행될 리시버용 인텐트
        val shortcutIntent = Intent(this, BusAlertReceiver::class.java).apply {
            action = if (type == "RIDE") "com.czt.bbt.START_RIDE" else "com.czt.bbt.START_ARRIVAL"
            putExtra("alert_id", id)
        }

        // 런처에 반환할 숏컷 인텐트 구성
        val resultIntent = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, label)
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, com.czt.bbt.R.mipmap.ic_launcher))
            
            // 삼성 루틴용 추가 라벨
            putExtra("intent_label", label)
        }
        
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
