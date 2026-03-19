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
                    val fullStartStr = SimpleDateFormat("yyyy-MM-dd (E) HH:mm", Locale.KOREAN).format(Date(log.boardingTime))

                    // 예상 또는 실제 하차 정보 계산
                    val alightDisplay: String
                    val durationDisplay: String
                    if (log.alightTime != null) {
                        alightDisplay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.alightTime))
                        durationDisplay = " (${(log.alightTime - log.boardingTime) / 60000}분)"
                    } else {
                        // 이행 중일 땐 DB에 저장된 API 기반 예상시간 사용, 없으면 경과시간 표시
                        val estMin = log.estimatedMinutes ?: ((System.currentTimeMillis() - log.boardingTime) / 60000).toInt().coerceAtLeast(1)
                        val estArrivalTime = log.boardingTime + (estMin * 60000L)
                        alightDisplay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(estArrivalTime))
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
                    Text("$fullStartStr ~ $alightDisplay$durationDisplay", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("승차: ${log.boardingStationName.replace("(", " [").replace(")", "]")}", fontSize = 13.sp)
                    Text(
                        text = if (isOngoing) "목적지: ${log.alightStationName ?: "확인 중"} (이동 중)"
                        else "하차: ${log.alightStationName?.replace("(", " [")?.replace(")", "]") ?: "미정"}",
                        fontSize = 13.sp,
                        color = if (isOngoing) MaterialTheme.colorScheme.secondary else Color.Unspecified
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (!isOngoing) {
                            IconButton(onClick = {
                                val dStr = SimpleDateFormat("yyyy-MM-dd (E)", Locale.KOREAN).format(Date(log.boardingTime))
                                val bTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.boardingTime))
                                val aTime = if (log.alightTime != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.alightTime)) else "??"
                                val dur = if (log.alightTime != null) " (${(log.alightTime - log.boardingTime) / 60000}분)" else ""

                                val shareText = "[버스알림 이력]\n일자: $dStr\n버스: ${log.busNumber} (${log.plateNumber ?: "차량미확인"})\n승차: ${log.boardingStationName} ($bTime)\n하차: ${log.alightStationName ?: "미정"} ($aTime)\n소요시간: $dur"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "이력 공유하기"))
                            }) { Icon(Icons.Default.Share, "공유", tint = MaterialTheme.colorScheme.primary) }
                        }

                        IconButton(onClick = { viewModel.deleteHistory(log) }) {
                            Icon(Icons.Default.Delete, "삭제", tint = MaterialTheme.colorScheme.error)
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
