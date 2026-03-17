package com.onbada.seathermo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.onbada.seathermo.application.di.ApplicationDIContainer
import com.onbada.seathermo.presentation.AppNavigation

/**
 * 앱의 유일한 메인 Activity.
 *
 * [개념] 단일 액티비티 아키텍처(Single Activity Architecture)를 따릅니다.
 *        모든 화면 전환은 이 Activity 위에서 Jetpack Compose Navigation(NavHost)을 통해 이루어집니다.
 *        iOS의 @main SeaThermoApp 구조체 역할에 대응합니다.
 *
 * [앱 흐름] Splash → (Onboarding) → MainTab
 */
class MainActivity : ComponentActivity() {

    // [개념] DI 컨테이너를 Activity 레벨에서 소유합니다.
    //        lateinit var는 나중에 초기화할 것을 컴파일러에게 알립니다.
    //        iOS의 private let applicationDIContainer: ApplicationDIContainer에 대응합니다.
    private lateinit var diContainer: ApplicationDIContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        // [개념] installSplashScreen()은 반드시 super.onCreate() 이전에 호출해야 합니다.
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // DI 컨테이너 초기화
        diContainer = ApplicationDIContainer(this)

        // Compose 첫 프레임이 그려질 때까지 시스템 Splash를 화면에 유지합니다.
        // [개념] setKeepOnScreenCondition { true } 동안 시스템 Splash가 계속 표시됩니다.
        //        Compose가 첫 프레임을 그리면 isComposeReady = true가 되고 시스템 Splash가 해제됩니다.
        //        이렇게 하면 사용자는 시스템 Splash(파란 배경+아이콘)에서 우리 SplashScreen.kt로
        //        끊김 없이 전환되는 것처럼 보입니다.
        //        Android 12+에서 초기 흰 화면 또는 다른 배경이 노출되는 것을 방지합니다.
        var isComposeReady by mutableStateOf(false)
        splashScreen.setKeepOnScreenCondition { !isComposeReady }

        enableEdgeToEdge()

        setContent {
            // Compose 트리가 처음 그려진 직후 시스템 Splash를 해제합니다.
            // [개념] LocalView.current.viewTreeObserver로 첫 프레임 완료 시점을 감지합니다.
            val view = LocalView.current
            if (!view.isInEditMode) {
                view.viewTreeObserver.addOnPreDrawListener {
                    isComposeReady = true
                    true
                }
            }

            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AppNavigation(diContainer = diContainer)
                }
            }
        }
    }
}
