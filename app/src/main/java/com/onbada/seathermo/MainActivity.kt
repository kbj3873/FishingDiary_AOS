package com.onbada.seathermo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

// [개념] Compose 앱에서는 AppCompatActivity 대신 ComponentActivity를 사용합니다.
//        setContentView(R.layout.xml) 대신 setContent { } 블록 안에 Composable을 선언합니다.
//        Swift의 @main App 구조체에서 WindowGroup { ContentView() }를 선언하는 것과 동일합니다.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [개념] enableEdgeToEdge()는 상태바/내비게이션바 영역까지 앱 콘텐츠가 확장되도록 설정합니다.
        //        iOS의 .ignoresSafeArea()와 유사하며, Scaffold의 paddingValues로 안전 영역을 제어합니다.
        enableEdgeToEdge()

        // [개념] setContent { }는 이 Activity의 UI 루트를 Compose로 선언합니다.
        //        이 블록 안에 NavHost 또는 최상위 Screen Composable을 배치합니다.
        setContent {
            // TODO: NavHost 및 앱 테마 적용 예정
        }
    }
}
