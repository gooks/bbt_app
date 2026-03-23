package com.czt.bbt

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.czt.bbt.BuildConfig

@HiltAndroidApp
class BusApp : Application() {
    companion object {
        var keyHash: String = "추출 중..."
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Kakao SDK
        KakaoSdk.init(this, "91e667c325c3df2dd6c843138a86fefe")
        
        // 카카오 키 해시 추출 및 저장
        try {
            keyHash = com.kakao.sdk.common.util.Utility.getKeyHash(this)
            android.util.Log.e("KAKAO_HASH", "************************************************")
            android.util.Log.e("KAKAO_HASH", "[KAKAO_KEY_HASH]: $keyHash")
            android.util.Log.e("KAKAO_HASH", "************************************************")
        } catch (e: Exception) {
            android.util.Log.e("KAKAO_HASH", "키 해시 추출 실패", e)
            keyHash = "추출 실패: ${e.message}"
        }

        // Initialize Firebase
        try {
            com.google.firebase.FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Timber.e("Firebase 초기화 실패: ${e.message}")
        }
    }
}
