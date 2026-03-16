package com.czt.bbt

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import com.czt.bbt.BuildConfig

@HiltAndroidApp
class BusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Kakao SDK
        KakaoSdk.init(this, "91e667c325c3df2dd6c843138a86fefe")
    }
}
