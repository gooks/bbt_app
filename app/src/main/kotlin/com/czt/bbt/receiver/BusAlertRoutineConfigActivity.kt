package com.czt.bbt.receiver

import android.app.Activity
import android.content.Intent
import android.os.Build
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
import com.czt.bbt.service.BusAlertService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// 1. 실제 루틴 실행 시 호출될 중계 액티비티 (투명)
@AndroidEntryPoint
class BusAlertRoutineExecutionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent.action
        
        // 삼성 루틴은 타입을 변환할 수 있으므로 Long과 String 모두 체크
        val alertId = intent.getLongExtra("alert_id", -1L).takeIf { it != -1L }
            ?: intent.getStringExtra("alert_id")?.toLongOrNull() ?: -1L

        if (alertId != -1L) {
            val serviceAction = when (action) {
                "com.czt.bbt.START_RIDE" -> BusAlertService.ACTION_START_RIDE
                "com.czt.bbt.START_ARRIVAL" -> BusAlertService.ACTION_START_ARRIVAL
                else -> null
            }

            if (serviceAction != null) {
                val serviceIntent = Intent(this, BusAlertService::class.java).apply {
                    this.action = serviceAction
                    putExtra(BusAlertService.EXTRA_ALERT_ID, alertId)
                }
                
                // 이미 실행 중인지 여부는 서비스 내부에서 판단하여 처리하므로 즉시 요청
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }
        }
        finish()
    }
}

// 2. 설정 액티비티 (기존 로직 유지하되 숏컷 대상 변경)
@AndroidEntryPoint
open class BusAlertRoutineConfigActivity : ComponentActivity() {
    @Inject lateinit var database: BusDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isRideMode = this.javaClass.simpleName.contains("Ride")
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RoutineConfigScreen(isRideMode)
                }
            }
        }
    }

    @Composable
    fun RoutineConfigScreen(isRideMode: Boolean) {
        var rideAlerts by remember { mutableStateOf<List<RideAlert>>(emptyList()) }
        var arrivalAlerts by remember { mutableStateOf<List<ArrivalAlert>>(emptyList()) }

        LaunchedEffect(Unit) {
            if (isRideMode) {
                rideAlerts = database.busDao().getAllRideAlerts().first()
            } else {
                arrivalAlerts = database.busDao().getAllArrivalAlerts().first()
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isRideMode) "버스이동알림 선택" else "버스도착알림 선택",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (isRideMode) {
                    items(rideAlerts) { alert ->
                        ListItem(
                            headlineContent = { Text("${alert.busNumber}번 (${alert.destinationStationName}행)") },
                            supportingContent = { Text("ID: ${alert.id}") },
                            modifier = Modifier.clickable { selectForRoutine(alert.id, "RIDE", alert.busNumber) }
                        )
                    }
                } else {
                    items(arrivalAlerts) { alert ->
                        ListItem(
                            headlineContent = { Text("${alert.stationName} (${alert.targetBusNames.joinToString(", ")})") },
                            supportingContent = { Text("ID: ${alert.id}") },
                            modifier = Modifier.clickable { selectForRoutine(alert.id, "ARRIVAL", alert.stationName) }
                        )
                    }
                }
            }
        }
    }

    private fun selectForRoutine(id: Long, type: String, name: String) {
        val label = if (type == "RIDE") "이동:$name 시작" else "도착:$name 시작"
        
        // 숏컷 인텐트 대상을 리시버가 아닌 중계 액티비티(ExecutionActivity)로 설정
        val shortcutIntent = Intent(this, BusAlertRoutineExecutionActivity::class.java).apply {
            action = if (type == "RIDE") "com.czt.bbt.START_RIDE" else "com.czt.bbt.START_ARRIVAL"
            putExtra("alert_id", id)
            // 액티비티 호출을 위한 플래그
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val resultIntent = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, label)
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this@BusAlertRoutineConfigActivity, com.czt.bbt.R.mipmap.ic_launcher))
            
            // 삼성 루틴용 메타데이터
            putExtra("intent_label", label)
        }
        
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}

@AndroidEntryPoint
class BusAlertRideRoutineConfigActivity : BusAlertRoutineConfigActivity()

@AndroidEntryPoint
class BusAlertArrivalRoutineConfigActivity : BusAlertRoutineConfigActivity()
