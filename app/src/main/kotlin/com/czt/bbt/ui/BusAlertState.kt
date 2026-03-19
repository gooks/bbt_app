package com.czt.bbt.ui

import kotlinx.coroutines.flow.MutableStateFlow

object BusAlertState {
    val liveStatusFlow = MutableStateFlow<Map<Long, String>>(emptyMap())
    val liveStatusDetailFlow = MutableStateFlow<Map<Long, String>>(emptyMap())
    val activeIdsFlow = MutableStateFlow<List<Long>>(emptyList())
    val apiUsageFlow = MutableStateFlow<Map<String, Int>>(emptyMap()) // API 실시간 카운트 추가
}
