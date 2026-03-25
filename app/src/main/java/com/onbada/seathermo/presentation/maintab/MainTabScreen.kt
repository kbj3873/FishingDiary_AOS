package com.onbada.seathermo.presentation.maintab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onbada.seathermo.R
import com.onbada.seathermo.application.di.ApplicationDIContainer
import com.onbada.seathermo.presentation.currenttemperature.screen.CrawlingCurrentTemperatureScreen
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.CrawlingCurrentTemperatureViewModel
import com.onbada.seathermo.presentation.fishingrecord.screen.FishingRecordScreen
import com.onbada.seathermo.presentation.fishingrecord.viewmodel.FishingRecordViewModel
import com.onbada.seathermo.presentation.seaanalysis.screen.SeaAnalysisScreen
import com.onbada.seathermo.infrastructure.webview.WebPage
import com.onbada.seathermo.presentation.history.screen.HistoryTabNavHost
import com.onbada.seathermo.presentation.setting.screen.SettingScreen
import com.onbada.seathermo.presentation.setting.viewmodel.SettingViewModel

// 탭 활성/비활성 색상 — iOS MainTabView.swift의 accentColor(#2563EB), 비활성(#8E8E93)과 동일
private val TabActiveColor = Color(0xFF2563EB)
private val TabInactiveColor = Color(0xFF8E8E93)

// 탭바 배경: Figma 기준 rgba(255,255,255,0.8) = #CCFFFFFF
private val TabBarBackgroundColor = Color(0xCCFFFFFF)

// 탭바 상단 구분선: Figma 기준 rgba(0,0,0,0.1) = #1A000000
private val TabBarDividerColor = Color(0x1A000000)

/**
 * 앱의 메인 하단 탭 화면.
 *
 * [개념] 하단 탭 내비게이션(BottomNavigation)을 통해 5개의 주요 화면을 전환하는 컨테이너 역할을 합니다.
 *        iOS의 MainTabView.swift 로직을 Jetpack Compose 환경에 맞게 이식했습니다.
 *        SwiftUI의 TabView와 동일한 역할을 수행합니다.
 */
@Composable
fun MainTabScreen(
    diContainer: ApplicationDIContainer,
    // SeaAnalysisDetailScreen으로 이동하는 콜백.
    // [개념] 콜백 파라미터로 Navigation 의존성을 AppNavigation에 위임합니다.
    //        MainTabScreen은 navController를 직접 보유하지 않아 결합도를 낮춥니다.
    //        iOS의 @EnvironmentObject로 전달하는 패턴과 동일한 의도입니다.
    onNavigateToSeaAnalysisDetail: (stationCode: String) -> Unit = {},
    onNavigateToWebPage: (WebPage) -> Unit = {}
) {
    // 현재 선택된 탭 인덱스 상태 관리.
    // [개념] remember { mutableIntStateOf(0) } 는 화면이 다시 그려져도(Recomposition)
    //        값을 유지하는 상태 변수입니다. Swift의 @State와 동일합니다.
    // [개념] rememberSaveable은 remember와 달리 Navigation 백스택 복귀 시에도
    //        상태를 복원합니다. SeaAnalysisDetailScreen에서 뒤로가기 시
    //        마지막으로 선택된 탭(수온분석=1)이 유지됩니다.
    //        iOS의 @State는 뷰가 살아있는 동안 상태를 유지하는 것과 동일한 효과입니다.
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // 방문한 탭 인덱스 집합 — 최초 방문 시에만 Composable을 생성하고 이후 재사용합니다.
    // [개념] rememberSaveable로 화면 회전 시에도 방문 이력이 복원됩니다.
    //        초기값에 selectedTabIndex를 포함하여 복원된 탭도 즉시 렌더링합니다.
    //        List<Int>는 Bundle에 저장 가능한 타입입니다.
    var visitedTabIndices by rememberSaveable { mutableStateOf(listOf(selectedTabIndex)) }

    // 탭 전환 시 해당 인덱스를 방문 목록에 추가합니다.
    // [개념] LaunchedEffect(selectedTabIndex)는 selectedTabIndex가 바뀔 때마다 실행됩니다.
    //        한 번 추가된 탭은 제거되지 않으므로 Composable이 유지됩니다.
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex !in visitedTabIndices) {
            visitedTabIndices = visitedTabIndices + selectedTabIndex
        }
    }

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
            // 상단 구분선 + NavigationBar를 Column으로 묶어
            // Figma의 border-t 효과를 구현합니다.
            Column {
                // Figma: border-t-[0.909px] rgba(0,0,0,0.1)
                HorizontalDivider(
                    thickness = 1.dp,
                    color = TabBarDividerColor
                )
                // [개념] NavigationBar는 Material3의 하단 탭 바 컴포넌트입니다.
                //        tonalElevation = 0.dp 로 Material3 기본 색조 오버레이를 제거합니다.
                NavigationBar(
                    containerColor = TabBarBackgroundColor,
                    tonalElevation = 0.dp
                ) {
                    tabItems.forEachIndexed { index, item ->
                        val isSelected = selectedTabIndex == index

                        // [개념] NavigationBarItem은 개별 탭 버튼입니다.
                        //        NavigationBarItemDefaults.colors()로 활성/비활성 색상을 지정하고
                        //        indicatorColor = Transparent 으로 선택 시 나타나는 pill 배경을 제거합니다.
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTabIndex = index },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TabActiveColor,
                                selectedTextColor = TabActiveColor,
                                unselectedIconColor = TabInactiveColor,
                                unselectedTextColor = TabInactiveColor,
                                // Figma 디자인에 pill(선택 배경 원형) 없음 → 투명 처리
                                indicatorColor = Color.Transparent
                            ),
                            icon = {
                                // [개념] painterResource()로 Vector Drawable을 불러옵니다.
                                //        tint는 NavigationBarItemDefaults.colors()가 자동 적용합니다.
                                Icon(
                                    painter = painterResource(id = item.iconResId),
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                // Figma: 10sp, Regular, letterSpacing 0.12sp
                                Text(
                                    text = item.title,
                                    style = TextStyle(
                                        fontSize = 10.sp,
                                        letterSpacing = 0.12.sp
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        // 하단 탭 바 높이를 제외한 영역에 콘텐츠 배치
        Box(modifier = Modifier.padding(paddingValues)) {

            // 탭 렌더링 전략: Lazy + Persistent
            // [개념] iOS의 TabView는 처음 진입한 탭만 생성하고 이후 전환 시 재사용합니다.
            //        여기서도 동일하게 구현합니다:
            //        1. 아직 방문하지 않은 탭(visitedTabIndices에 없는)은 Composition에 포함하지 않음
            //           → 앱 시작 시 미방문 탭의 부수 효과(GPS 등)가 발생하지 않음
            //        2. 한 번 방문한 탭은 숨겨져도 Composition에 계속 유지
            //           → 탭 복귀 시 지도/경로/타이머 등 상태가 보존됨
            //        3. ViewModel은 탭 블록 안에서 생성 → 방문 시점에 최초 1회 생성 후 재사용
            tabItems.forEachIndexed { index, item ->
                // 아직 방문하지 않은 탭은 Composition에 포함하지 않습니다.
                if (index !in visitedTabIndices) return@forEachIndexed

                val isVisible = selectedTabIndex == index

                // [개념] alpha(0f)는 화면을 투명하게 만들지만 Composable은 Composition에 유지됩니다.
                //        zIndex로 선택된 탭을 항상 최상단에 배치하여 터치 이벤트를 가장 먼저 처리합니다.
                //        숨겨진 탭은 선택된 탭 아래에 깔리므로 터치를 가로채지 않습니다.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (isVisible) 1f else 0f)
                        .zIndex(if (isVisible) 1f else 0f)
                ) {
                    // [개념] viewModel()은 탭 블록 안에 위치하므로 해당 탭 최초 방문 시점에 생성됩니다.
                    //        Composable이 Composition에 남아 있는 한 동일 인스턴스가 재사용됩니다.
                    when (item) {
                        TabItem.CurrentTemperature -> {
                            val crawlingViewModel: CrawlingCurrentTemperatureViewModel = viewModel(
                                factory = diContainer.makeCrawlingCurrentTemperatureViewModelFactory()
                            )
                            CrawlingCurrentTemperatureScreen(
                                viewModel = crawlingViewModel,
                                diContainer = diContainer
                            )
                        }
                        TabItem.Analysis -> SeaAnalysisScreen(
                            diContainer = diContainer,
                            onNavigateToDetail = { _, stationCode ->
                                onNavigateToSeaAnalysisDetail(stationCode)
                            }
                        )
                        TabItem.FishingRecord -> {
                            val fishingRecordViewModel: FishingRecordViewModel = viewModel(
                                factory = diContainer.makeFishingRecordViewModelFactory()
                            )
                            FishingRecordScreen(viewModel = fishingRecordViewModel)
                        }
                        TabItem.History -> HistoryTabNavHost(
                            diContainer = diContainer,
                            isVisible = isVisible
                        )
                        TabItem.Settings -> {
                            val settingViewModel: SettingViewModel = viewModel(
                                factory = diContainer.makeSettingViewModelFactory()
                            )
                            SettingScreen(
                                viewModel = settingViewModel,
                                onNavigateToWebPage = onNavigateToWebPage
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 탭 메뉴 항목을 정의하는 sealed class.
 *
 * [개념] sealed class는 정해진 하위 타입만 가질 수 있는 제한된 클래스 계층입니다.
 *        Swift의 enum과 유사하지만 각 케이스가 독립적인 object/class가 될 수 있습니다.
 */
sealed class TabItem(val title: String, val iconResId: Int) {
    object CurrentTemperature : TabItem("현재수온", R.drawable.ic_tab_temperature)
    object Analysis : TabItem("수온분석", R.drawable.ic_tab_analysis)
    object FishingRecord : TabItem("낚시기록", R.drawable.ic_tab_fishing)
    object History : TabItem("히스토리", R.drawable.ic_tab_history)
    object Settings : TabItem("설정", R.drawable.ic_tab_settings)
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
