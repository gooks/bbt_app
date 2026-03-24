package com.czt.bbt.ui.screens

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
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
                Text("설정", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 16.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { currentSubScreen = "SETTINGS" }.padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("계정설정", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("구글/카카오 계정 연동 및 자동 알림 설정을 관리합니다.") },
                        trailingContent = { Icon(Icons.Default.Settings, null) }
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
                    modifier = Modifier.fillMaxWidth().clickable { currentSubScreen = "TTS" }.padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    ListItem(
                        headlineContent = { Text("TTS 실행", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("목소리 선택 및 실시간 하이라이트 기능을 실행합니다.") },
                        trailingContent = { Icon(Icons.Default.PlayArrow, null) }
                    )
                }
            }
        }
        "SETTINGS" -> {
            LaunchedEffect(Unit) { viewModel.checkKakaoLoginStatus() }
            SettingsScreen(viewModel) { currentSubScreen = "MENU" }
        }
        "TTS" -> TtsTestScreen(tts, wordRange, availableVoices, selectedVoice) { currentSubScreen = "MENU" }
        "API" -> ApiUsageScreen(viewModel) { currentSubScreen = "MENU" }
    }
}

@Composable
fun SettingsScreen(viewModel: BusViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var appPassword by remember { mutableStateOf(viewModel.googleAppPassword.value) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("계정설정", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // 1. 구글 계정 연동 섹션
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("구글 계정 연동", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("계정 연동 시 설정 동기화 및 메일 자동 발송이 가능합니다.", fontSize = 12.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (viewModel.isGoogleLoggedIn.value) {
                    ListItem(
                        headlineContent = { Text(viewModel.googleEmail.value) },
                        supportingContent = { Text("연동됨 (설정 동기화 활성)") },
                        leadingContent = { Icon(Icons.Default.AccountCircle, null, tint = MaterialTheme.colorScheme.primary) }
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { 
                                viewModel.forceSyncAndRefresh()
                                android.widget.Toast.makeText(context, "클라우드 데이터 동기화 완료!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Text(" 동기화")
                        }
                        
                        Button(
                            onClick = { 
                                viewModel.logoutGoogle()
                                android.widget.Toast.makeText(context, "구글 연동이 해제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("연동 해제")
                        }
                    }
                } else {
                    Button(
                        onClick = { (context as? com.czt.bbt.MainActivity)?.loginWithGoogle() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4))
                    ) {
                        Text("Google 계정 연동하기")
                    }
                }
            }
        }

        // 2. 카카오 계정 연동 섹션
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("카카오톡 연동", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("카카오톡 '나에게 보내기' 알림을 받으려면 연동이 필요합니다.", fontSize = 12.sp, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (viewModel.isKakaoLoggedIn.value) {
                    ListItem(
                        headlineContent = { Text(viewModel.kakaoNickname.value) },
                        supportingContent = { Text("연동됨 (카톡 알림 활성)") },
                        leadingContent = { Icon(Icons.Default.Email, null, tint = Color(0xFFFFE812)) }
                    )
                    
                    Button(
                        onClick = { 
                            viewModel.logoutKakao()
                            android.widget.Toast.makeText(context, "카카오톡 연동이 해제되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("연동 해제")
                    }
                } else {
                    Button(
                        onClick = { viewModel.loginWithKakao(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE812), contentColor = Color.Black)
                    ) {
                        Text("카카오톡 계정 연동하기")
                    }
                }
            }
        }

        // 3. 이메일 자동 발송 설정 섹션
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = if (viewModel.isGoogleLoggedIn.value) CardDefaults.cardColors() else CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("메일 자동 발송 설정", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                
                if (viewModel.isGoogleLoggedIn.value) {
                    Text("발신 계정: ${viewModel.googleEmail.value}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = appPassword,
                        onValueChange = { appPassword = it },
                        label = { Text("구글 앱 비밀번호 (16자리)") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        singleLine = true,
                        placeholder = { Text("abcd efgh ijkl mnop") }
                    )
                    
                    Text(
                        "Gmail 설정 > 보안 > 2단계 인증 > 앱 비밀번호에서 발급받은 코드를 입력하고 저장하세요.",
                        fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { 
                            viewModel.saveGoogleAccount(viewModel.googleEmail.value, appPassword)
                            android.widget.Toast.makeText(context, "메일 발송 설정이 저장되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("메일 설정 저장")
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("위의 구글 계정 연동을 먼저 완료해주세요.", fontSize = 13.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
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
