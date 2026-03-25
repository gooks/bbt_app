package com.czt.bbt.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun ArrivalStatusScreen(viewModel: BusViewModel) {
    val activeIds by viewModel.arrivalLiveStatusIds.collectAsState()
    val liveDetail by viewModel.arrivalLiveStatusDetail.collectAsState() // 상세 정보 구독
    val arrivalAlerts by viewModel.arrivalAlerts.collectAsState()
    
    val listState = rememberLazyListState()
    
    // 드래그 앤 드롭 상태 관리
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // 특정 알림으로 스크롤 이동 처리
    val coroutineScope = rememberCoroutineScope()
    val scrollToAlertId by viewModel.scrollToAlertId
    
    LaunchedEffect(scrollToAlertId) {
        scrollToAlertId?.let { targetId ->
            val index = activeIds.indexOf(targetId)
            if (index != -1) {
                listState.animateScrollToItem(index)
            }
            viewModel.setScrollToAlertId(null) // 처리 완료 후 초기화
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("실시간 도착 현황", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (activeIds.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("현재 실행 중인 도착 알림이 없습니다.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(items = activeIds, key = { _, id -> id }) { index: Int, id: Long ->
                    val statusText = liveDetail[id] ?: "도착 정보를 불러오는 중입니다..."
                    
                    val isDragging = draggedItemIndex == index
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { dragOffset = 0f; draggedItemIndex = index },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        
                                        val threshold = 100f
                                        if (dragOffset > threshold && index < activeIds.size - 1) {
                                            viewModel.moveArrivalItem(index, index + 1)
                                            draggedItemIndex = index + 1
                                            dragOffset = 0f
                                        } else if (dragOffset < -threshold && index > 0) {
                                            viewModel.moveArrivalItem(index, index - 1)
                                            draggedItemIndex = index - 1
                                            dragOffset = 0f
                                        }
                                    },
                                    onDragEnd = { draggedItemIndex = null },
                                    onDragCancel = { draggedItemIndex = null }
                                )
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                    ) {
                        Row(
                            modifier = Modifier
                                .clickable { viewModel.refreshArrivalAlert(id) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = statusText,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Drag to reorder",
                                tint = Color.LightGray,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
