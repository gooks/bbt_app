package com.czt.bbt.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czt.bbt.ui.BusViewModel
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.clickable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.czt.bbt.model.RideHistory

@Composable
fun HistoryScreen(viewModel: BusViewModel) {
    val history by viewModel.history.collectAsState()
    val activeRideId by viewModel.activeRideAlertId
    val context = LocalContext.current
    var selectedHistory by remember { mutableStateOf<RideHistory?>(null) }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(history) { log ->
            val isOngoing = log.alightTime == null
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .clickable { selectedHistory = log },
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
                        // 운행 중일 때 예상 시간 계산 (정류장당 2분 가공치 사용)
                        // 실제 서비스에서는 destIndex - boardIndex 등을 활용하지만, 여기선 이력 데이터 기반으로 추정
                        val estMin = 30 // 기본 30분 예시 (실제로는 저장된 데이터를 기반으로 하거나 API 호출 필요)
                        val estArrivalTime = log.boardingTime + (estMin * 60000L)
                        alightDisplay = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(estArrivalTime))
                        durationDisplay = " (${estMin}분 소요 예상)"
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOngoing) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text("🔵 알림 중", fontSize = 10.sp) },
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

    selectedHistory?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedHistory = null },
            title = { Text("주행 상세 기록", fontWeight = FontWeight.Bold) },
            text = {
                val fullDate = SimpleDateFormat("yyyy년 MM월 dd일 (E)", Locale.KOREA).format(Date(log.boardingTime))
                val boardTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.boardingTime))
                val alightTime = if (log.alightTime != null) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.alightTime)) else "기록 없음"
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("날짜", fullDate)
                    DetailRow("버스 노선", "${log.busNumber}번")
                    DetailRow("차량 번호", log.plateNumber ?: "미확인")
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    DetailRow("승차 정류장", log.boardingStationName)
                    DetailRow("승차 시간", boardTime)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    DetailRow("하차 정류장", log.alightStationName ?: "기록 없음")
                    DetailRow("하차 시간", alightTime)
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedHistory = null }) { Text("닫기") }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
