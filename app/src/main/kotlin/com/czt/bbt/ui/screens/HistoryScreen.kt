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
    val context = LocalContext.current
    var selectedHistory by remember { mutableStateOf<RideHistory?>(null) }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(history) { log ->
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .clickable { selectedHistory = log },
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(log.boardingTime))
                    val boardStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.boardingTime))
                    val alightStr = if (log.alightTime != null) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(log.alightTime)) else "운행 중"
                    
                    Text(dateStr, style = MaterialTheme.typography.labelSmall)
                    Text("${log.busNumber}번 (${log.plateNumber ?: "차량미확인"})", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("승차: ${log.boardingStationName} ($boardStr)", fontSize = 14.sp)
                    Text("하차: ${log.alightStationName ?: "진행 중"} ($alightStr)", fontSize = 14.sp)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = {
                            val shareText = "[버스알림]\n날짜: $dateStr\n버스: ${log.busNumber} (${log.plateNumber ?: "차량미확인"})\n승차: ${log.boardingStationName} ($boardStr)\n하차: ${log.alightStationName ?: "미정"} ($alightStr)"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "이력 공유하기"))
                        }) { Icon(Icons.Default.Share, "공유", tint = MaterialTheme.colorScheme.primary) }
                        
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
