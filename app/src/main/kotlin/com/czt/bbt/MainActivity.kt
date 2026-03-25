package com.czt.bbt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.czt.bbt.data.BusRepository
import com.czt.bbt.ui.BusAppScreen
import com.czt.bbt.ui.BusViewModel
import com.czt.bbt.util.ShortcutUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    @Inject
    lateinit var repository: BusRepository

    private val viewModel: BusViewModel by viewModels()
    private var tts: TextToSpeech? = null
    private val _currentWordRange = mutableStateOf<Pair<Int, Int>?>(null)
    private val _availableVoices = mutableStateListOf<Voice>()
    private val _selectedVoice = mutableStateOf<Voice?>(null)

    private lateinit var connectivityManager: ConnectivityManager
    private val networkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                android.util.Log.d("NetworkCallback", "Network Available: Reconciling with cloud.")
                lifecycleScope.launch {
                    repository.reconcileWithCloud()
                    repository.registerRealtimeSync() // Re-register listener on network recovery
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                android.util.Log.d("NetworkCallback", "Network Lost: Realtime sync will be disabled.")
                repository.detachRealtimeSync() // Detach listener when network is lost
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            Toast.makeText(this, "서비스 이용을 위해 모든 권한이 필요합니다.", Toast.LENGTH_LONG).show()
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("GoogleLogin", "Result Code: ${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                android.util.Log.d("GoogleLogin", "Google Sign-In Success: ${account?.email}")

                val idToken = account?.idToken
                if (idToken != null) {
                    val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                    com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener(this) { firebaseAuthTask ->
                            if (firebaseAuthTask.isSuccessful) {
                                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                val firebaseUid = firebaseUser?.uid
                                android.util.Log.d("FirebaseAuth", "Firebase Auth Success. UID: $firebaseUid")
                                viewModel.onGoogleSignInSuccess(account?.email ?: "", firebaseUid ?: "")
                                Toast.makeText(this, "구글 로그인 성공 (Firebase 연동): ${account?.email}", Toast.LENGTH_SHORT).show()
                                // After successful sign-in, trigger reconciliation
                                lifecycleScope.launch {
                                    repository.reconcileWithCloud()
                                }
                            } else {
                                android.util.Log.e("FirebaseAuth", "Firebase Auth Failed: ${firebaseAuthTask.exception?.message}")
                                Toast.makeText(this, "Firebase 연동 실패: ${firebaseAuthTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                } else {
                    android.util.Log.e("GoogleLogin", "Google ID Token is null. Cannot proceed with Firebase Auth.")
                    Toast.makeText(this, "구글 ID 토큰 없음: Firebase 연동 불가", Toast.LENGTH_LONG).show()
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                android.util.Log.e("GoogleLogin", "Error Code: ${e.statusCode}, Message: ${e.message}")
                val errorMsg = when(e.statusCode) {
                    7 -> "네트워크 연결 확인 필요 (7)"
                    10 -> "인증 오류(10): Firebase에 SHA-1 지문이 등록되었는지, google-services.json이 최신인지 확인하세요."
                    12500 -> "구글 서비스 업데이트 필요 (12500)"
                    12501 -> "사용자가 로그인을 취소했습니다 (12501)"
                    else -> "로그인 실패: ${e.statusCode}"
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
            }
        } else if (result.resultCode == RESULT_CANCELED) {
            android.util.Log.w("GoogleLogin", "Sign-in cancelled by user or system configuration error.")
            Toast.makeText(this, "로그인이 취소되었습니다. 설정(SHA-1)을 확인해 주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    fun loginWithGoogle() {
        android.util.Log.d("GoogleLogin", "Starting Google Sign-In Intent")
        val webClientIdResId = resources.getIdentifier("default_web_client_id", "string", packageName)
        val webClientId = if (webClientIdResId != 0) getString(webClientIdResId) else null
        android.util.Log.d("GoogleLogin", "Web Client ID: $webClientId")

        val gsoBuilder = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        
        if (webClientId != null) {
            gsoBuilder.requestIdToken(webClientId)
        }

        val client = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gsoBuilder.build())
        client.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("APP_START", "MainActivity onCreate called.")
        tts = TextToSpeech(this, this)
        
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        combine(viewModel.rideAlerts, viewModel.arrivalAlerts) { ride, arrival ->
                            Pair(ride, arrival)
                        }.collect { (ride, arrival) ->
                            ShortcutUtil.updateShortcuts(this@MainActivity, ride, arrival)
                        }
                    }

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
        
        checkAndRequestPermissions()
        val prefs = getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE)
        val googleUserId = prefs.getString("google_user_id", "")
        if (googleUserId.isNullOrEmpty()) {
            android.util.Log.d("MainActivity", "onCreate: Google User ID is empty. Initiating Google Sign-In.")
            loginWithGoogle()
        } else {
            android.util.Log.d("MainActivity", "onCreate: Google User ID exists. Triggering reconciliation.")
            lifecycleScope.launch {
                repository.reconcileWithCloud()
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        repository.registerRealtimeSync()
    }
    
    override fun onStop() {
        super.onStop()
        repository.detachRealtimeSync()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.setLanguage(Locale.KOREAN)
            val allVoices = tts?.voices ?: emptySet()
            val filteredVoices = allVoices.filter { it.locale.language == "ko" }
            _availableVoices.clear()
            _availableVoices.addAll(filteredVoices)

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
        repository.detachRealtimeSync() // Final cleanup
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onDestroy()
    }
}
