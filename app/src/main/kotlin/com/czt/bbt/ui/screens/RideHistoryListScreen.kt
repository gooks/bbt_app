package com.czt.bbt.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czt.bbt.ui.BusViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryListScreen(viewModel: BusViewModel, onBack: () -> Unit) {
    var busNoFilter by viewModel.historyBusNoFilter
    var startDate by viewModel.historyStartDate
    var endDate by viewModel.historyEndDate
    val results = viewModel.filteredHistory
    val selectedIds = viewModel.historySelectedIds
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxHeight()) {
                    Text("조회 조건 설정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = busNoFilter,
                        onValueChange = { busNoFilter = it },
                        label = { Text("버스번호") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("번호 포함 검색") }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("탑승 기간", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("시작일(yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("종료일(yyyy-MM-dd)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Button(
                        onClick = { 
                            viewModel.searchRideHistory()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("조건 적용 및 조회")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                    TopAppBar(
                        title = { Text("이동이력 관리", fontSize = 18.sp) },
                        navigationIcon = { 
                            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "뒤로") } 
                        },
                        actions = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.List, "필터")
                            }
                        }
                    )
                    // 버튼 영역을 제목 아래에 배치
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ActionBtn("조회", modifier = Modifier.weight(1f)) { viewModel.searchRideHistory() }
                        ActionBtn("삭제", color = Color.Red, enabled = selectedIds.isNotEmpty(), modifier = Modifier.weight(1f)) { 
                            viewModel.deleteSelectedHistories() 
                        }
                        ActionBtn("출력", modifier = Modifier.weight(1f)) { viewModel.exportToCsv(false) }
                        ActionBtn("전체출력", modifier = Modifier.weight(1.2f)) { viewModel.exportToCsv(true) }
                        ActionBtn("메일", modifier = Modifier.weight(1f)) { viewModel.sendHistoryEmail() }
                    }
                    Divider()
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                val scrollState = rememberScrollState()
                Box(modifier = Modifier.fillMaxSize().horizontalScroll(scrollState)) {
                    Column {
                        Row(
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = results.isNotEmpty() && selectedIds.size == results.size,
                                onCheckedChange = { checked ->
                                    selectedIds.clear()
                                    if (checked) selectedIds.addAll(results.map { it.id })
                                }
                            )
                            TableHeader("일자", 120.dp)
                            TableHeader("승차시간", 80.dp)
                            TableHeader("하차시간", 80.dp)
                            TableHeader("소요시간", 100.dp)
                            TableHeader("버스번호", 80.dp)
                            TableHeader("차량번호", 120.dp)
                            TableHeader("승차정류장", 200.dp)
                            TableHeader("하차정류장", 200.dp)
                        }

                        Divider()

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(results) { item ->
                                val isSelected = selectedIds.contains(item.id)
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedIds.add(item.id) else selectedIds.remove(item.id)
                                        }
                                    )
                                    
                                    val dateWithDay = "${item.date}(${item.dayOfWeek})"
                                    val boardingStr = SimpleDateFormat("HH:mm").format(Date(item.boardingTime))
                                    val alightStr = item.alightTime?.let { SimpleDateFormat("HH:mm").format(Date(it)) } ?: "-"
                                    
                                    val durationStr = if (item.durationMinutes != null) {
                                        val h = item.durationMinutes / 60
                                        val m = item.durationMinutes % 60
                                        if (h > 0) "${h}시간 ${m}분" else "${m}분"
                                    } else "-"

                                    TableCell(dateWithDay, 120.dp)
                                    TableCell(boardingStr, 80.dp)
                                    TableCell(alightStr, 80.dp)
                                    TableCell(durationStr, 100.dp)
                                    TableCell(item.busNumber, 80.dp)
                                    TableCell(item.plateNumber ?: "-", 120.dp)
                                    TableCell(item.boardingStationName, 200.dp)
                                    TableCell(item.alightStationName ?: "-", 200.dp)
                                }
                                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        if (results.isEmpty()) viewModel.searchRideHistory()
    }
}

@Composable
fun ActionBtn(text: String, color: Color = Color.Unspecified, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgColor = if (enabled) MaterialTheme.colorScheme.surface else Color.Transparent
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 2.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = bgColor,
            contentColor = if (enabled) (if (color == Color.Unspecified) MaterialTheme.colorScheme.primary else color) else Color.Gray
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (enabled) (if (color == Color.Unspecified) MaterialTheme.colorScheme.outline else color.copy(alpha = 0.5f)) else Color.LightGray)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun TableHeader(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        maxLines = 1
    )
}

@Composable
fun TableCell(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text = text,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        fontSize = 13.sp,
        maxLines = 1
    )
}
