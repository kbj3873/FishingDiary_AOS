package com.onbada.seathermo.presentation.maintab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.onbada.seathermo.R

/**
 * 앱의 메인 하단 탭 화면.
 *
 * [개념] 하단 탭 내비게이션(BottomNavigation)을 통해 5개의 주요 화면을 전환하는 컨테이너 역할을 합니다.
 *        iOS의 MainTabView.swift 로직을 Jetpack Compose 환경에 맞게 이식했습니다.
 *        SwiftUI의 TabView와 동일한 역할을 수행합니다.
 */
@Composable
fun MainTabScreen() {
    // 현재 선택된 탭 인덱스 상태 관리.
    // [개념] remember { mutableIntStateOf(0) } 는 화면이 다시 그려져도(Recomposition)
    //        값을 유지하는 상태 변수입니다. Swift의 @State와 동일합니다.
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // 탭 메뉴 항목 정의
    val tabItems = listOf(
        TabItem.CurrentTemperature,
        TabItem.Analysis,
        TabItem.FishingRecord,
        TabItem.History,
        TabItem.Settings
    )

    // [개념] Scaffold는 앱의 표준 UI 구조(상단바, 하단바 등)를 쉽게 배치할 수 있는 레이아웃 틀입니다.
    Scaffold(
        bottomBar = {
            // [개념] NavigationBar는 하단 탭 바 컴포넌트입니다.
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp // 상단 경계 구분을 위한 고도 효과
            ) {
                tabItems.forEachIndexed { index, item ->
                    // [개념] NavigationBarItem은 개별 탭 버튼입니다.
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                painter = painterResource(id = item.iconResId),
                                contentDescription = item.title,
                                tint = if (selectedTabIndex == index) Color(0xFF2563EB) else Color(0xFF8E8E93)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                color = if (selectedTabIndex == index) Color(0xFF2563EB) else Color(0xFF8E8E93)
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        // 하단 탭 바 높이를 제외한 영역에 콘텐츠 배치
        Box(modifier = Modifier.padding(paddingValues)) {
            // 선택된 탭에 따라 다른 화면을 표시합니다.
            // [개념] when 절을 통해 선택된 인덱스에 맞는 Composable을 노출합니다.
            //        Swift의 switch case 문과 동일한 제어문입니다.
            when (tabItems[selectedTabIndex]) {
                TabItem.CurrentTemperature -> {
                    // TODO: CurrentTemperatureScreen 구현 후 연결
                    PlaceholderScreen("현재수온")
                }
                TabItem.Analysis -> {
                    // TODO: SeaAnalysisScreen 구현 후 연결
                    PlaceholderScreen("수온분석")
                }
                TabItem.FishingRecord -> {
                    // TODO: FishingRecordScreen 구현 후 연결
                    PlaceholderScreen("낚시기록")
                }
                TabItem.History -> {
                    // TODO: HistoryScreen 구현 후 연결
                    PlaceholderScreen("히스토리")
                }
                TabItem.Settings -> {
                    // TODO: SettingScreen 구현 후 연결
                    PlaceholderScreen("설정")
                }
            }
        }
    }
}

/**
 * 탭 메뉴 항목을 정의하는 클래스.
 */
sealed class TabItem(val title: String, val iconResId: Int) {
    // [주의] Compose의 painterResource()는 mipmap을 지원하지 않습니다. drawable만 사용 가능합니다.
    object CurrentTemperature : TabItem("현재수온", com.onbada.seathermo.R.drawable.ic_launcher_foreground) // 임시 아이콘
    object Analysis : TabItem("수온분석", com.onbada.seathermo.R.drawable.ic_launcher_foreground)
    object FishingRecord : TabItem("낚시기록", com.onbada.seathermo.R.drawable.ic_launcher_foreground)
    object History : TabItem("히스토리", com.onbada.seathermo.R.drawable.ic_launcher_foreground)
    object Settings : TabItem("설정", com.onbada.seathermo.R.drawable.ic_launcher_foreground)
}

/**
 * 구현 예정 화면을 위한 임시 플레이스홀더 화면.
 */
@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(text = "구현 예정", color = Color.Gray)
        }
    }
}
