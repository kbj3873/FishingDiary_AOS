package com.onbada.seathermo

import android.app.Application
import com.kakao.vectormap.KakaoMapSdk
import com.onbada.seathermo.managers.FDAppManager

class FDApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 앱 매니저 초기화 — SharedPreferences에서 저장된 설정값(지도 타입 등)을 로드합니다.
        // iOS의 FDAppManager.init()에서 UserDefaults를 로드하는 시점에 대응합니다.
        FDAppManager.getInstance().appInitialize(this)

        // Initialize Kakao Maps SDK
        try {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_APP_KEY)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
