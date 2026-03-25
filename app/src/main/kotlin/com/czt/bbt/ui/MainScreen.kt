package com.czt.bbt.ui

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.czt.bbt.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusAppScreen(
    viewModel: BusViewModel, 
    tts: TextToSpeech?, 
    wordRange: State<Pair<Int, Int>?>,
    availableVoices: List<Voice>,
    selectedVoice: MutableState<Voice?>
) {
    var tabIndex by viewModel.selectedTabIndex
    val tabs = listOf("도착현황", "버스도착", "이동이력", "버스이동", "설정")

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("버스알림", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                TabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title) })
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (tabIndex) {
                0 -> ArrivalStatusScreen(viewModel)
                1 -> ArrivalAlertScreen(viewModel)
                2 -> HistoryScreen(viewModel)
                3 -> RideAlertScreen(viewModel)
                4 -> LabScreen(viewModel, tts, wordRange, availableVoices, selectedVoice)
            }
        }
    }
}
