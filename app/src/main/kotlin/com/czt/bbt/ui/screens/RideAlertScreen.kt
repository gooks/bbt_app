package com.czt.bbt.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czt.bbt.ui.BusViewModel

@Composable
fun RideAlertScreen(viewModel: BusViewModel) {
    val alerts by viewModel.rideAlerts.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(alerts) { alert ->
                Card(modifier = Modifier.padding(8.dp).fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("ID: ${alert.id}", fontSize = 10.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${alert.busNumber}번", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("→ ${alert.destinationStationName}", fontSize = 16.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                            val isRunning = viewModel.activeRideAlertId.value == alert.id
                            if (isRunning) {
                                OutlinedButton(
                                    onClick = { viewModel.stopRideAlert() },
                                    modifier = Modifier.padding(end = 8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Icon(Icons.Default.Clear, null)
                                    Text(" 알림중지")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.startRideAlert(alert) },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null)
                                    Text(" 알림시작")
                                }
                            }
                            IconButton(onClick = { viewModel.setEditRideAlert(alert); showDialog = true }) { Icon(Icons.Default.Edit, "수정") }
                            IconButton(onClick = { viewModel.deleteRideAlert(alert) }) { Icon(Icons.Default.Delete, "삭제", tint = Color.Red) }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { viewModel.resetRideForm(); showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) { Icon(Icons.Default.Add, "추가", tint = Color.White) }
    }

    if (showDialog) RideAlertDialog(viewModel) { showDialog = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideAlertDialog(viewModel: BusViewModel, onDismiss: () -> Unit) {
    val isEdit = viewModel.editingRideAlert.value != null
    AlertDialog(
        onDismissRequest = { viewModel.resetRideForm(); onDismiss() },
        title = { Text(if (isEdit) "알림 수정" else "이동 알림 추가") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()) // 스크롤 추가하여 깨짐 방지
            ) {
                if (viewModel.rideSelectedBus.value == null) {
                    OutlinedTextField(
                        value = viewModel.rideBusSearchQuery.value,
                        onValueChange = { viewModel.rideBusSearchQuery.value = it },
                        label = { Text("버스 번호 입력") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { viewModel.searchBusForRide() }) { Icon(Icons.Default.Search, null) } }
                    )
                    if (viewModel.isLoading.value) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    
                    // 내부 LazyColumn 대신 Column + forEach로 변경 (중첩 스크롤 이슈 방지)
                    viewModel.rideBusSearchResult.forEach { bus ->
                        ListItem(
                            headlineContent = { Text("${bus.name} (${bus.type})") },
                            supportingContent = { Text(bus.region) },
                            modifier = Modifier.clickable { viewModel.selectBusForRide(bus) }
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("선택된 버스: ", style = MaterialTheme.typography.bodyMedium)
                        Text("${viewModel.rideSelectedBus.value!!.name}번", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.rideSelectedBus.value = null }) { Text("변경") }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("하차 정류장 선택:", style = MaterialTheme.typography.labelLarge)
                    
                    // 정류장 목록을 카드 형태의 리스트로 표시 (높이 제한)
                    Surface(
                        modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth(),
                        tonalElevation = 1.dp,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            viewModel.rideRouteStations.forEach { station ->
                                val isSelected = viewModel.rideSelectedDestination.value?.id == station.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.rideSelectedDestination.value = station }
                                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${station.seq}. ${station.name}",
                                        modifier = Modifier.weight(1f),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("알림 및 공유 설정", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    
                    // 카카오톡 설정 섹션 (나에게 자동 알림)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, null, tint = Color(0xFFFBC02D), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("카카오톡 나에게 자동 알림", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Switch(checked = viewModel.rideShareKakao.value, onCheckedChange = { viewModel.rideShareKakao.value = it })
                            }
                            
                            if (viewModel.rideShareKakao.value) {
                                Text(
                                    "✨ 승/하차 시 [현재 정보]와 함께 [오늘의 전체 주행 기록]을\n   본인의 카톡으로 자동 전송합니다.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF795548),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // 이메일 설정 섹션 (자동 전송 강조)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("📧 이메일 자동 알림 (백그라운드 전송)", style = MaterialTheme.typography.labelLarge)
                            OutlinedTextField(
                                value = viewModel.rideTempEmail.value,
                                onValueChange = { viewModel.rideTempEmail.value = it },
                                label = { Text("수신 이메일 주소") },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                trailingIcon = { IconButton(onClick = { viewModel.addShareEmail() }) { Icon(Icons.Default.Add, null) } },
                                singleLine = true
                            )
                            
                            viewModel.rideShareEmails.forEach { email ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                    Icon(Icons.Default.Email, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(email, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.removeShareEmail(email) }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.saveRideAlert(); onDismiss() }, enabled = viewModel.rideSelectedDestination.value != null) {
                Text(if (isEdit) "수정 완료" else "추가")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
