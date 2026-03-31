package com.czt.bbt.ui.screens

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czt.bbt.model.RideHistory
import com.czt.bbt.ui.BusViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: BusViewModel) {
    val history by viewModel.history.collectAsState()
    val context = LocalContext.current

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(history) { log ->
            val isOngoing = log.alightTime == null
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .clickable { /* 상세 정보 팝업 등 추가 가능 */ },
                elevation = CardDefaults.cardElevation(if (isOngoing) 4.dp else 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOngoing) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val dateDisplay = "${log.date} (${log.dayOfWeek})"
                    val startTimeDisplay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.boardingTime))

                    // 하차 정보 표시 조합
                    val endTimeDisplay: String
                    val durationDisplay: String
                    if (log.alightTime != null) {
                        endTimeDisplay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.alightTime))
                        durationDisplay = " (${log.durationMinutes ?: ((log.alightTime - log.boardingTime) / 60000)}분)"
                    } else {
                        val estMin = log.estimatedMinutes ?: ((System.currentTimeMillis() - log.boardingTime) / 60000).toInt().coerceAtLeast(1)
                        val estArrivalTime = log.boardingTime + (estMin * 60000L)
                        endTimeDisplay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(estArrivalTime))
                        durationDisplay = " (${estMin}분 소요예정)"
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOngoing) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text("🚍 알림 중", fontSize = 10.sp) },
                                modifier = Modifier.height(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("${log.busNumber}번 (${log.plateNumber ?: "차량미확인"})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$dateDisplay $startTimeDisplay ~ $endTimeDisplay$durationDisplay", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

                    val bName = if (!log.boardingStationNo.isNullOrEmpty()) "[${log.boardingStationNo}]${log.boardingStationName}" else log.boardingStationName
                    val aName = if (!log.alightStationNo.isNullOrEmpty()) "[${log.alightStationNo}]${log.alightStationName ?: ""}" else (log.alightStationName ?: "")

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("승차정류장: $bName", fontSize = 13.sp)
                    Text(
                        text = if (isOngoing) "목적정류장: ${if (aName.isEmpty()) "확인 중" else aName} (이동 중)"
                        else "하차정류장: ${if (aName.isEmpty()) "미정" else aName}",
                        fontSize = 13.sp,
                        color = if (isOngoing) MaterialTheme.colorScheme.secondary else Color.Unspecified
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (!isOngoing) {
                            IconButton(onClick = {
                                val dStr = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(log.boardingTime))
                                val bTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.boardingTime))
                                val aTime = if (log.alightTime != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.alightTime)) else "탑승중"
                                val dur = if (log.alightTime != null) "${(log.alightTime - log.boardingTime) / 60000}분" else "미확정"

                                val shareText = "[버스알림 이력]\n일자: $dStr\n버스: ${log.busNumber} (${log.plateNumber ?: "차량미확인"})\n승차: $bName ($bTime)\n하차: ${if (aName.isEmpty()) "미정" else aName} ($aTime)\n소요시간: $dur"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "이력 공유하기"))
                            }) { Icon(Icons.Default.Share, "공유", tint = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
