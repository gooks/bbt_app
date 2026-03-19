package com.czt.bbt.ui.screens

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.czt.bbt.ui.BusViewModel

@Composable
fun LabScreen(
    viewModel: BusViewModel,
    tts: TextToSpeech?, 
    wordRange: State<Pair<Int, Int>?>,
    availableVoices: List<Voice>,
    selectedVoice: MutableState<Voice?>
) {
    var currentSubScreen by remember { mutableStateOf("MENU") }

    when (currentSubScreen) {
        "MENU" -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("실험실", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { currentSubScreen = "TTS" }.padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("TTS 테스트", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("목소리 선택 및 실시간 하이라이트 기능을 테스트합니다.") },
                        trailingContent = { Icon(Icons.Default.ArrowForward, null) }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { 
                        viewModel.loadApiUsage()
                        currentSubScreen = "API" 
                    }.padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("API 현황", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("오늘 남은 API 호출 횟수를 확인합니다. (자정 초기화)") },
                        trailingContent = { Icon(Icons.Default.Info, null) }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth().clickable { /* 기타 테스트 */ }.padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("기타 테스트", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("준비 중인 기능입니다.") },
                        trailingContent = { Icon(Icons.Default.Build, null) }
                    )
                }
            }
        }
        "TTS" -> TtsTestScreen(tts, wordRange, availableVoices, selectedVoice) { currentSubScreen = "MENU" }
        "API" -> ApiUsageScreen(viewModel) { currentSubScreen = "MENU" }
    }
}

@Composable
fun ApiUsageScreen(viewModel: BusViewModel, onBack: () -> Unit) {
    val usage = viewModel.apiUsage

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("API 일일 사용 현황", style = MaterialTheme.typography.titleLarge)
        }
        
        Text(
            "자정마다 1000회로 초기화됩니다. 호출 시 숫자가 감소합니다.",
            fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(start = 48.dp, bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(usage.keys.sorted()) { tag ->
                val count = usage[tag] ?: 1000
                Card(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tag, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text(
                            "$count / 1000", 
                            color = if (count < 100) Color.Red else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsTestScreen(
    tts: TextToSpeech?, 
    wordRange: State<Pair<Int, Int>?>, 
    availableVoices: List<Voice>,
    selectedVoice: MutableState<Voice?>,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var displayedText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("TTS 설정 및 테스트", style = MaterialTheme.typography.titleLarge)
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            OutlinedTextField(
                value = selectedVoice.value?.name ?: "기본 목소리",
                onValueChange = {},
                readOnly = true,
                label = { Text("TTS 목소리 선택") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableVoices.forEach { voice ->
                    DropdownMenuItem(
                        text = { Text(voice.name) },
                        onClick = {
                            selectedVoice.value = voice
                            tts?.voice = voice
                            expanded = false
                        }
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("읽을 문장을 입력하세요") },
            modifier = Modifier.fillMaxWidth().height(120.dp).padding(top = 16.dp)
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    displayedText = inputText
                    val params = Bundle()
                    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "test_id")
                    tts?.speak(displayedText, TextToSpeech.QUEUE_FLUSH, params, "test_id")
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Text(" 말하기")
            }
            OutlinedButton(
                onClick = {
                    inputText = ""
                    displayedText = ""
                    tts?.stop()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, null)
                Text(" 초기화")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("출력 (실시간 하이라이트):", style = MaterialTheme.typography.labelLarge)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.LightGray.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            val annotatedString = buildAnnotatedString {
                val range = wordRange.value
                if (range != null && displayedText.isNotEmpty() && range.first < displayedText.length) {
                    append(displayedText.substring(0, range.first))
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)) {
                        val end = if (range.second <= displayedText.length) range.second else displayedText.length
                        append(displayedText.substring(range.first, end))
                    }
                    if (range.second < displayedText.length) {
                        append(displayedText.substring(range.second))
                    }
                } else {
                    append(displayedText)
                }
            }
            Text(text = annotatedString, style = MaterialTheme.typography.bodyLarge, lineHeight = 30.sp)
        }
    }
}
