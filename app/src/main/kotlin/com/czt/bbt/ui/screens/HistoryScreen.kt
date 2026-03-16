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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czt.bbt.ui.BusViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: BusViewModel) {
    val history by viewModel.history.collectAsState()
    val context = LocalContext.current
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(history) { log ->
            Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
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
}
