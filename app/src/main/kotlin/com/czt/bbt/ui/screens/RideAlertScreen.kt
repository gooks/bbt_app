package com.czt.bbt.ui.screens

import androidx.compose.foundation.background
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
            Column(modifier = Modifier.fillMaxWidth()) {
                if (viewModel.rideSelectedBus.value == null) {
                    OutlinedTextField(
                        value = viewModel.rideBusSearchQuery.value,
                        onValueChange = { viewModel.rideBusSearchQuery.value = it },
                        label = { Text("버스 번호 입력") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { viewModel.searchBusForRide() }) { Icon(Icons.Default.Search, null) } }
                    )
                    if (viewModel.isLoading.value) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(viewModel.rideBusSearchResult) { bus ->
                            ListItem(
                                headlineContent = { Text("${bus.name} (${bus.type})") },
                                supportingContent = { Text(bus.region) },
                                modifier = Modifier.clickable { viewModel.selectBusForRide(bus) }
                            )
                        }
                    }
                } else {
                    Text("선택된 버스: ${viewModel.rideSelectedBus.value!!.name}번", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { viewModel.rideSelectedBus.value = null }) { Text("다시 선택") }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("하차 정류장 지정:", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.height(150.dp)) {
                        items(viewModel.rideRouteStations) { station ->
                            val isSelected = viewModel.rideSelectedDestination.value?.id == station.id
                            Text(
                                text = "${station.seq}. ${station.name}",
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.rideSelectedDestination.value = station }.background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent).padding(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("실시간 정보 공유 (승차/하차 시)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Icon(Icons.Default.Notifications, null, tint = Color(0xFFFEE500), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("카카오톡 공유 사용", modifier = Modifier.weight(1f))
                        Switch(checked = viewModel.rideShareKakao.value, onCheckedChange = { viewModel.rideShareKakao.value = it })
                    }
                    if (viewModel.rideShareKakao.value) {
                        OutlinedTextField(
                            value = viewModel.rideShareMemo.value,
                            onValueChange = { viewModel.rideShareMemo.value = it },
                            label = { Text("누구에게 공유할까요? (메모)") },
                            placeholder = { Text("예: 엄마, 친구들") },
                            modifier = Modifier.fillMaxWidth().padding(start = 28.dp, bottom = 8.dp),
                            singleLine = true
                        )
                        Text("* 승/하차 감지 시 카톡 친구 선택 창이 나타납니다.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 28.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.rideTempEmail.value,
                        onValueChange = { viewModel.rideTempEmail.value = it },
                        label = { Text("자동 공유 이메일 주소") },
                        placeholder = { Text("example@email.com") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { viewModel.addShareEmail() }) { Icon(Icons.Default.Add, null) } }
                    )
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        viewModel.rideShareEmails.forEach { email ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(email, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.removeShareEmail(email) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, tint = Color.Red, modifier = Modifier.size(16.dp))
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
