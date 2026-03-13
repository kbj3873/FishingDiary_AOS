package com.onbada.seathermo

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk

class FDApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Kakao Maps SDK
        try {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_APP_KEY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
