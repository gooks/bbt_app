package com.czt.bbt

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.czt.bbt.ui.BusAppScreen
import com.czt.bbt.ui.BusViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val _currentWordRange = mutableStateOf<Pair<Int, Int>?>(null)
    private val _availableVoices = mutableStateListOf<Voice>()
    private val _selectedVoice = mutableStateOf<Voice?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "서비스 이용을 위해 모든 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        checkAndRequestPermissions()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val viewModel: BusViewModel = hiltViewModel()
                    BusAppScreen(
                        viewModel = viewModel, 
                        tts = tts, 
                        wordRange = _currentWordRange,
                        availableVoices = _availableVoices,
                        selectedVoice = _selectedVoice
                    )
                }
            }
        }
    }
override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
        tts?.setLanguage(Locale.KOREAN)

        // 사용 가능한 한국어 목소리 필터링
        val allVoices = tts?.voices ?: emptySet()
        val filteredVoices = allVoices.filter { it.locale.language == "ko" }
        _availableVoices.clear()
        _availableVoices.addAll(filteredVoices)

        // 특정 목소리(ko-kr-x-koc-local)를 기본값으로 설정 시도
        val targetVoice = filteredVoices.find { it.name == "ko-kr-x-koc-local" }
        if (targetVoice != null) {
            _selectedVoice.value = targetVoice
            tts?.voice = targetVoice
        } else {
            _selectedVoice.value = tts?.voice
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { _currentWordRange.value = null }
                override fun onError(utteranceId: String?) {}
                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    _currentWordRange.value = Pair(start, end)
                }
            })
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
        }.toTypedArray()

        val missing = permissions.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissionLauncher.launch(missing.toTypedArray())
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
