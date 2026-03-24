package com.onbada.seathermo.presentation.currenttemperature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onbada.seathermo.R
import com.onbada.seathermo.application.di.ApplicationDIContainer
import com.onbada.seathermo.domain.entity.CombinedCurrentTemperature
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.CrawlingCurrentTemperatureViewModel
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.CrawlingOceanSelectViewModel

// ── 색상 상수 ────────────────────────────────────────────────────────────────
private val ScreenBackground = Color(0xFFF2F2F7)
private val HeaderBackground = Color.White
private val HeaderDividerColor = Color(0xFFE5E5EA)
private val AccentBlue = Color(0xFF2563EB)
private val TextGray = Color(0xFF8E8E93)
private val TextLightGray = Color(0xFFAEAEB2)
private val CardBackground = Color.White

// OceanRegionCard 그라디언트 배경
// Figma: #4A7C9E → #5A8CAE → #6A9CBE (top to bottom)
private val CrawlingCardGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF4A7C9E),
        Color(0xFF5A8CAE),
        Color(0xFF6A9CBE)
    )
)

/**
 * 크롤링 기반 바다 현재수온 메인 화면.
 *
 * [개념] Crawling 버전은 즐겨찾기된 Region 목록에 대해
 *        risaInfo API를 병렬로 호출하여 수온 데이터를 가져옵니다.
 *        iOS의 CrawlingCurrentTemperatureView.swift에 대응합니다.
 *
 * @param viewModel 크롤링 기반 현재 수온 ViewModel
 * @param diContainer CrawlingOceanSelectViewModel 생성에 필요한 DI 컨테이너
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrawlingCurrentTemperatureScreen(
    viewModel: CrawlingCurrentTemperatureViewModel,
    diContainer: ApplicationDIContainer
) {
    // [개념] collectAsStateWithLifecycle()은 StateFlow를 Compose State로 변환합니다.
    //        앱이 백그라운드 상태가 되면 수집을 자동으로 중단하여 리소스를 절약합니다.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 헤더 ────────────────────────────────────────────────────────
            CrawlingHeaderSection()

            // ── 콘텐츠 ──────────────────────────────────────────────────────
            // [개념] PullToRefreshBox는 iOS의 .refreshable { } 모디파이어에 대응합니다.
            //        isRefreshing 으로 로딩 인디케이터 표시 여부를, onRefresh 로 갱신 동작을 지정합니다.
            //        내부 콘텐츠가 verticalScroll 또는 LazyColumn 이어야 당기기 제스처가 동작합니다.
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.fetchStationList() },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 16.dp)
                ) {
                    // 지역 추가/관리 버튼
                    CrawlingAddRegionButton(
                        onClick = { viewModel.setOceanSelectPresented(true) }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // [개념] when 조건 분기: 즐겨찾기 없으면 EmptyState, 있으면 카드 목록 표시
                    if (uiState.oceanStations.isEmpty() && !uiState.isLoading) {
                        CrawlingEmptyState()
                    } else {
                        // 즐겨찾기 지역 카드 목록
                        uiState.oceanStations.forEach { station ->
                            CrawlingOceanRegionCardView(station = station)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        // ── CrawlingOceanSelect BottomSheet ─────────────────────────────────
        // [개념] isOceanSelectPresented 상태가 true 일 때만 Sheet Composable을 트리에 추가합니다.
        //        iOS의 .sheet(isPresented: $viewModel.isOceanSelectPresented) { ... }에 대응합니다.
        if (uiState.isOceanSelectPresented) {
            val oceanSelectViewModel: CrawlingOceanSelectViewModel = viewModel(
                factory = diContainer.makeCrawlingOceanSelectViewModelFactory()
            )
            CrawlingOceanSelectScreen(
                viewModel = oceanSelectViewModel,
                onDismiss = { viewModel.setOceanSelectPresented(false) },
                onDataUpdated = { viewModel.onDataUpdated() }
            )
        }
    }
}

// ── 헤더 섹션 ────────────────────────────────────────────────────────────────

@Composable
private fun CrawlingHeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBackground)
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp, bottom = 12.dp)
    ) {
        // 제목: Figma 30sp, Bold, Black, letterSpacing -0.35sp
        Text(
            text = "바다 현재수온",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = (-0.35).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 부제목: 14sp, Regular, #8E8E93, letterSpacing -0.15sp
        Text(
            text = "즐겨찾기한 지역의 실시간 수온 정보",
            fontSize = 14.sp,
            color = TextGray,
            letterSpacing = (-0.15).sp
        )
    }
    // 하단 구분선 — #E5E5EA, 0.5dp
    HorizontalDivider(thickness = 0.5.dp, color = HeaderDividerColor)
}

// ── 지역 추가/관리 버튼 ───────────────────────────────────────────────────────

@Composable
private fun CrawlingAddRegionButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus),
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "지역 추가/관리",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = AccentBlue,
                letterSpacing = (-0.31).sp
            )
        }
    }
}

// ── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun CrawlingEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 빈 별 아이콘 — tint #E5E5EA
            Icon(
                painter = painterResource(id = R.drawable.ic_star_empty),
                contentDescription = null,
                tint = Color(0xFFE5E5EA),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // 주 메시지: 16sp, #8E8E93
            Text(
                text = "즐겨찾기한 지역이 없습니다",
                fontSize = 16.sp,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(4.dp))
            // 보조 메시지: 14sp, #AEAEB2
            Text(
                text = "위 버튼을 눌러 지역을 추가해보세요",
                fontSize = 14.sp,
                color = TextLightGray
            )
        }
    }
}

// ── OceanRegionCard ───────────────────────────────────────────────────────────

/**
 * 즐겨찾기 지역의 수온 정보를 표시하는 카드. (Crawling 버전)
 *
 * CurrentTemperatureScreen의 OceanRegionCardView와 동일한 UI입니다.
 * 데이터 소스만 다릅니다 (크롤링 vs Open API).
 *
 * @param station 표시할 관측소 수온 데이터
 */
@Composable
private fun CrawlingOceanRegionCardView(station: CombinedCurrentTemperature) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(brush = CrawlingCardGradient)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // ── 좌측: 수온 정보 ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "표층",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = (-0.15).sp
                    )
                    val surText = if (station.surTemperature.isNotEmpty()) "${station.surTemperature}°" else "-°"
                    Text(
                        text = surText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.35.sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CrawlingSubTemperatureRow(label = "중층", value = station.midTemperature)
                    CrawlingSubTemperatureRow(label = "저층", value = station.botTemperature)
                }
            }

            // ── 우측: 지역명 + 해역명 ─────────────────────────────────────
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = station.stationName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.44).sp
                )
                Text(
                    text = station.seaName.ifEmpty { crawlingResolveSeaName(station.stationName) },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = (-0.15).sp
                )
            }
        }
    }
}

@Composable
private fun CrawlingSubTemperatureRow(label: String, value: String) {
    val isEmpty = value.isEmpty() || value == "0"
    if (isEmpty) {
        Text(
            text = "$label: 데이터 없음",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = (-0.15).sp
        )
    } else {
        // [개념] alignByBaseline()을 사용하여 폰트 굵기가 달라도 텍스트의 실제 하단 기준선을 완벽하게 일치시킵니다.
        Row {
            Text(
                text = "$label: ",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = (-0.44).sp,
                modifier = Modifier.alignByBaseline()
            )
            Text(
                text = "$value°",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = (-0.44).sp,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}

private fun crawlingResolveSeaName(stationName: String): String {
    val westSeaKeywords = listOf("서산", "목포", "군산", "인천", "태안", "보령", "부안", "영광", "무안", "신안", "백령도", "서천")
    val southSeaKeywords = listOf("여수", "통영", "거제", "남해", "완도", "고흥", "진도", "해남", "장흥", "강진", "사천", "서제주", "제주", "추자도", "보성", "진해")
    val eastSeaKeywords = listOf("울산", "포항", "동해", "강릉", "속초", "삼척", "울진", "영덕", "경주", "부산", "양양", "고성", "기장", "나곡", "덕천", "온양", "진하", "구룡포", "고리")

    if (westSeaKeywords.any { stationName.contains(it) }) return "서해"
    if (southSeaKeywords.any { stationName.contains(it) }) return "남해"
    if (eastSeaKeywords.any { stationName.contains(it) }) return "동해"
    return "해역"
}
