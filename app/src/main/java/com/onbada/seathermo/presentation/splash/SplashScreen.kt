package com.onbada.seathermo.presentation.splash

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onbada.seathermo.R
import com.onbada.seathermo.presentation.splash.viewmodel.SplashState
import com.onbada.seathermo.presentation.splash.viewmodel.SplashViewModel

// Play Store 링크 (향후 실제 앱 ID로 교체)
private const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.onbada.seathermo"

/**
 * 앱 시작 시 가장 먼저 표시되는 스플래시 화면.
 *
 * [개념] @Composable 어노테이션이 붙은 함수는 UI를 '선언'합니다.
 *        상태(state)가 변경되면 Compose 런타임이 해당 함수를 자동으로 재호출(Recomposition)합니다.
 *        SwiftUI의 SplashView에 대응합니다.
 *
 * [디자인] Figma 노드 112:44 기반
 *   - 배경: 수직 그라데이션 (#4FACFE → #3979BE → #1E3C72)
 *   - 아이콘: 130×130dp, 화면 중앙
 *
 * @param viewModel SplashViewModel — 버전 체크 및 관측소 목록 캐싱 담당
 * @param onNavigateToMain 메인 화면으로 이동하는 콜백
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel,
    onNavigateToMain: () -> Unit
) {
    val context = LocalContext.current

    // UI 상태를 Compose State로 변환하여 수집.
    // [개념] collectAsStateWithLifecycle()은 Flow를 Compose State로 변환합니다.
    //        화면이 백그라운드로 가면 자동으로 수집을 중단하여 불필요한 연산을 방지합니다.
    //        iOS의 @StateObject + @Published 조합과 동일한 역할입니다.
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Alert 표시 여부 및 메시지 상태
    // [개념] remember { mutableStateOf() }는 Recomposition 간 값을 유지하는 로컬 상태입니다.
    //        iOS의 @State private var와 동일합니다.
    var showForceUpdateDialog by remember { mutableStateOf(false) }
    var showOptionalUpdateDialog by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }

    // Figma 기반 수직 그라데이션 배경 브러시.
    // [개념] Brush.verticalGradient는 위에서 아래로 색상이 변하는 그라데이션을 정의합니다.
    //        colorStops의 첫 번째 값(0f~1f)은 그라데이션 전체 길이 대비 위치 비율입니다.
    val backgroundBrush = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to Color(0xFF4FACFE),     // 상단: 밝은 파랑
            0.459f to Color(0xFF3979BE), // 중간 (Figma 45.9% 위치): 중간 파랑
            1f to Color(0xFF1E3C72)      // 하단: 진한 네이비
        )
    )

    // Splash 화면 진입 시 한 번 실행 — 버전 체크 및 관측소 캐싱 시작.
    // [개념] LaunchedEffect(Unit)은 Composable이 화면에 나타날 때 한 번만 코루틴을 실행합니다.
    //        Unit을 key로 사용하면 Recomposition이 일어나도 재실행되지 않습니다.
    //        iOS의 .onAppear { } 모디파이어와 동일한 역할입니다.
    LaunchedEffect(Unit) {
        viewModel.onAppear()
    }

    // state 변화에 따라 화면 전환 또는 Alert 표시.
    // [개념] LaunchedEffect(state)는 state 값이 바뀔 때마다 코루틴을 재실행합니다.
    //        iOS의 .onChange(of: viewModel.state) { }에 대응합니다.
    LaunchedEffect(state) {
        when (val currentState = state) {
            is SplashState.ReadyToNavigate -> onNavigateToMain()
            is SplashState.ForceUpdate -> {
                alertMessage = currentState.message
                showForceUpdateDialog = true
            }
            is SplashState.OptionalUpdate -> {
                alertMessage = currentState.message
                showOptionalUpdateDialog = true
            }
            is SplashState.Loading -> Unit // 로딩 중 — 아무 동작 없음
        }
    }

    // 전체 화면을 그라데이션으로 채우고 아이콘을 중앙에 배치.
    // [개념] Box는 자식 Composable을 겹쳐서 배치하는 레이아웃입니다. SwiftUI의 ZStack에 대응합니다.
    //        fillMaxSize()는 부모의 최대 크기를 모두 차지합니다. iOS의 .frame(maxWidth: .infinity) 와 동일합니다.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        // Figma에서 직접 추출한 스플래시 아이콘 — 투명 배경의 물고기+온도계 흰색 아이콘.
        // [개념] painterResource()는 res/drawable의 이미지를 로드합니다.
        //        iOS의 Image("launchIcon")에 대응합니다.
        // [주의] launcher 아이콘(R.mipmap.ic_launcher_foreground)은 네이비 배경을 포함하므로 사용 금지.
        //        Figma 에셋에서 직접 추출한 splash_icon.png(투명 배경)를 사용해야 합니다.
        Image(
            painter = painterResource(id = R.drawable.splash_icon),
            contentDescription = null,
            modifier = Modifier.size(130.dp)
        )
    }

    // 강제 업데이트 Alert — 취소 불가, Play Store 이동만 가능.
    // [개념] AlertDialog는 Compose의 팝업 대화상자 컴포넌트입니다. iOS의 .alert()에 대응합니다.
    if (showForceUpdateDialog) {
        AlertDialog(
            onDismissRequest = { /* 강제 업데이트는 닫기 불가 */ },
            title = { Text("업데이트 필요") },
            text = { Text(alertMessage) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                    context.startActivity(intent)
                    // 앱이 포그라운드로 돌아와도 재표시
                    showForceUpdateDialog = true
                }) {
                    Text("Play Store로 이동")
                }
            }
        )
    }

    // 선택적 업데이트 Alert — 업데이트 또는 나중에 선택 가능.
    if (showOptionalUpdateDialog) {
        AlertDialog(
            onDismissRequest = { /* 배경 탭으로 닫기 불가 — 명시적 선택 유도 */ },
            title = { Text("업데이트 알림") },
            text = { Text(alertMessage) },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_URL))
                    context.startActivity(intent)
                }) {
                    Text("업데이트")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOptionalUpdateDialog = false
                    onNavigateToMain()
                }) {
                    Text("나중에")
                }
            }
        )
    }
}
