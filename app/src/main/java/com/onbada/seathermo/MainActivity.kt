package com.onbada.seathermo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.onbada.seathermo.presentation.maintab.MainTabScreen

/**
 * 앱의 유일한 메인 Activity.
 *
 * [개념] 단일 액티비티 아키텍처(Single Activity Architecture)를 따릅니다.
 *        모든 화면 전환은 이 Activity 위에서 Jetpack Compose Navigation(NavHost)을 통해 이루어집니다.
 *        iOS의 @main App 구조체 + WindowGroup { MainTabView() } 역할에 대응합니다.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [개념] enableEdgeToEdge()는 상태바와 내비게이션 바 영역까지 앱 UI를 확장합니다.
        //        Scaffold의 paddingValues를 사용하여 시스템 바와 겹치지 않게 UI를 배치할 수 있습니다.
        enableEdgeToEdge()

        // [개념] setContent는 Compose의 UI 선언 시작점입니다.
        //        이 안에서 Composable 함수를 호출하여 화면을 구성합니다.
        setContent {
            // [개념] MaterialTheme은 Google의 디자인 가이드인 Material 3 스타일을 앱에 적용합니다.
            //        색상, 타이포그래피, 모양 등을 전역적으로 관리합니다.
            MaterialTheme {
                // [개념] Surface는 배경색을 설정하고 하위 탭/버튼의 텍스트 색상을 적절히 조절하는 컨테이너입니다.
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 앱의 메인 하단 탭 화면을 시작점으로 설정합니다.
                    // 추후 Splash -> Onboarding -> MainTab 순서로 NavHost를 통해 연결할 예정입니다.
                    MainTabScreen()
                }
            }
        }
    }
}
