package com.czt.bbt.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czt.bbt.ui.BusViewModel
import com.czt.bbt.model.ArrivalAlert
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArrivalAlertScreen(viewModel: BusViewModel) {
    val alerts by viewModel.arrivalAlerts.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    
    // 드래그 앤 드롭 상태 관리
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(items = alerts, key = { _, alert -> alert.id }) { index: Int, alert: ArrivalAlert ->
                val isDragging = draggedItemIndex == index
                val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "elevation")
                
                Card(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .animateItemPlacement(),
                    elevation = CardDefaults.cardElevation(elevation)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ID: ${alert.id}", fontSize = 10.sp, color = Color.Gray)
                                Text(alert.alias ?: alert.stationName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                if (alert.alias != null) {
                                    Text(alert.stationName, fontSize = 14.sp, color = Color.Gray)
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Drag to reorder",
                                tint = if (isDragging) MaterialTheme.colorScheme.primary else Color.LightGray,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .pointerInput(index) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { dragOffset = 0f; draggedItemIndex = index },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffset += dragAmount.y
                                                
                                                val threshold = 80f
                                                if (dragOffset > threshold && index < alerts.size - 1) {
                                                    viewModel.moveArrivalAlert(index, index + 1)
                                                    draggedItemIndex = index + 1
                                                    dragOffset = 0f
                                                } else if (dragOffset < -threshold && index > 0) {
                                                    viewModel.moveArrivalAlert(index, index - 1)
                                                    draggedItemIndex = index - 1
                                                    dragOffset = 0f
                                                }
                                            },
                                            onDragEnd = { draggedItemIndex = null },
                                            onDragCancel = { draggedItemIndex = null }
                                        )
                                    }
                            )
                        }
                        Text("대상 버스: ${alert.targetBusNames.joinToString(", ")}", color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                            val isRunning = viewModel.activeArrivalIds.contains(alert.id)
                            if (isRunning) {
                                OutlinedButton(
                                    onClick = { viewModel.stopArrivalAlert(alert.id) },
                                    modifier = Modifier.padding(end = 8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                                ) {
                                    Icon(Icons.Default.Clear, null)
                                    Text(" 알림중지")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.startArrivalAlert(alert) },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null)
                                    Text(" 알림시작")
                                }
                            }
                            IconButton(onClick = { viewModel.setEditArrivalAlert(alert); showDialog = true }) { Icon(Icons.Default.Edit, "수정") }
                            IconButton(onClick = { viewModel.deleteArrivalAlert(alert) }) { Icon(Icons.Default.Delete, "삭제", tint = Color.Red) }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { viewModel.stopArrivalAll() }, 
                    modifier = Modifier.padding(16.dp).fillMaxWidth(), 
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("도착 알림 강제 중지")
                }
            }
        }
        FloatingActionButton(
            onClick = { viewModel.resetArrivalForm(); showDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.secondary
        ) { Icon(Icons.Default.Add, "추가", tint = Color.White) }
    }

    if (showDialog) ArrivalAlertDialog(viewModel) { showDialog = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrivalAlertDialog(viewModel: BusViewModel, onDismiss: () -> Unit) {
    val isEdit = viewModel.editingArrivalAlert.value != null
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { viewModel.resetArrivalForm(); onDismiss() },
        title = { Text(if (isEdit) "알림 수정" else "도착 알림 추가") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (viewModel.arrivalSelectedStation.value == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = viewModel.arrivalStationSearchQuery.value,
                            onValueChange = { viewModel.arrivalStationSearchQuery.value = it },
                            label = { Text("정류장명 검색") },
                            modifier = Modifier.weight(1f),
                            trailingIcon = { IconButton(onClick = { viewModel.searchStationForArrival() }) { Icon(Icons.Default.Search, null) } }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { scope.launch { viewModel.searchNearbyStationsForArrival() } }) {
                            Icon(Icons.Default.LocationOn, "내 주변", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (viewModel.isLoading.value) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(viewModel.arrivalStationSearchResult) { station ->
                            ListItem(
                                headlineContent = { Text(station.name) },
                                supportingContent = { Text("정류소 번호: ${station.no}") },
                                modifier = Modifier.clickable { viewModel.selectStationForArrival(station) }
                            )
                        }
                    }
                } else {
                    Text("선택된 정류장: ${viewModel.arrivalSelectedStation.value!!.name}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    TextButton(onClick = { viewModel.arrivalSelectedStation.value = null }) { Text("다시 선택") }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("알림 받을 버스 선택 (다중):", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.height(250.dp)) {
                        items(viewModel.arrivalBusList) { bus ->
                            val isSelected = viewModel.arrivalSelectedBuses.contains(bus.routeId)
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleArrivalBusSelection(bus) }.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = { viewModel.toggleArrivalBusSelection(bus) })
                                Text("${bus.name}번 (${bus.type})")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.arrivalAlias.value,
                        onValueChange = { viewModel.arrivalAlias.value = it },
                        label = { Text("알림 별칭 (옵션)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("예: 출근길, 집앞 등") }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.saveArrivalAlert(); onDismiss() }, enabled = viewModel.arrivalSelectedBuses.isNotEmpty()) {
                Text(if (isEdit) "수정 완료" else "추가")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}
