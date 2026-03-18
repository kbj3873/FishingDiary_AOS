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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.CurrentTemperatureViewModel
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.OceanSelectViewModel

// ── 색상 상수 ────────────────────────────────────────────────────────────────
// Figma 기준: 배경 #F2F2F7, 헤더 흰색, 구분선 #E5E5EA
private val ScreenBackground = Color(0xFFF2F2F7)
private val HeaderBackground = Color.White
private val HeaderDividerColor = Color(0xFFE5E5EA)
private val AccentBlue = Color(0xFF2563EB)
private val TextGray = Color(0xFF8E8E93)
private val TextLightGray = Color(0xFFAEAEB2)
private val CardBackground = Color.White

// OceanRegionCard 그라디언트 배경 (배경 이미지 없을 때 사용)
// Figma: #4A7C9E → #5A8CAE → #6A9CBE (top to bottom)
private val CardGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF4A7C9E),
        Color(0xFF5A8CAE),
        Color(0xFF6A9CBE)
    )
)

/**
 * 바다 현재수온 메인 화면.
 *
 * [개념] Composable은 상태(state)에 따라 UI를 '선언'합니다.
 *        uiState.oceanStations가 비어 있으면 EmptyState를,
 *        데이터가 있으면 카드 목록을 표시합니다.
 *        iOS의 CurrentTemperatureView.swift에 대응합니다.
 *
 * @param viewModel 현재 수온 데이터와 UI 상태를 관리하는 ViewModel
 */
@Composable
fun CurrentTemperatureScreen(
    viewModel: CurrentTemperatureViewModel,
    diContainer: ApplicationDIContainer
) {
    // [개념] collectAsStateWithLifecycle()은 StateFlow를 Compose State로 변환합니다.
    //        화면이 백그라운드로 가면 자동으로 수집을 중단하여 불필요한 연산을 막습니다.
    //        Swift의 @StateObject + @Published 조합과 동일한 역할입니다.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 헤더 ────────────────────────────────────────────────────────
            HeaderSection()

            // ── 콘텐츠 ──────────────────────────────────────────────────────
            // [개념] verticalScroll(rememberScrollState())은 스크롤 가능한 Column입니다.
            //        iOS의 ScrollView에 대응합니다.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                // 지역 추가/관리 버튼
                AddRegionButton(
                    onClick = { viewModel.setOceanSelectPresented(true) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // [개념] when은 Swift의 switch와 동일한 조건 분기입니다.
                if (uiState.oceanStations.isEmpty() && !uiState.isLoading) {
                    EmptyStateView()
                } else {
                    // 즐겨찾기 지역 카드 목록
                    // [개념] Column 안에서 items를 직접 반복합니다.
                    //        LazyColumn은 스크롤 가능한 Column 안에 중첩할 수 없기 때문에,
                    //        부모 verticalScroll이 있는 경우 forEach로 직접 렌더링합니다.
                    uiState.oceanStations.forEach { station ->
                        OceanRegionCardView(station = station)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // ── OceanSelect BottomSheet ─────────────────────────────────────────
        // [개념] isOceanSelectPresented가 true일 때만 Sheet를 Composable 트리에 추가합니다.
        //        iOS의 .sheet(isPresented: $viewModel.isOceanSelectPresented) { ... }에 대응합니다.
        if (uiState.isOceanSelectPresented) {
            // [개념] viewModel(factory = ...)로 OceanSelectViewModel을 생성합니다.
            //        Sheet가 열릴 때 한 번 생성되고, Sheet 범위 내에서 생명주기가 유지됩니다.
            val oceanSelectViewModel: OceanSelectViewModel = viewModel(
                factory = diContainer.makeOceanSelectViewModelFactory()
            )
            OceanSelectScreen(
                viewModel = oceanSelectViewModel,
                onDismiss = { viewModel.setOceanSelectPresented(false) },
                onDataUpdated = { viewModel.onDataUpdated() }
            )
        }
    }
}

// ── 헤더 섹션 ────────────────────────────────────────────────────────────────

/**
 * 화면 상단 헤더.
 *
 * Figma: 흰 배경, 상단 padding 48dp, 제목(30sp Bold), 부제목(14sp #8E8E93), 하단 구분선 0.5dp
 * iOS의 headerSection에 대응합니다.
 */
@Composable
private fun HeaderSection() {
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

        // 부제목: Figma 14sp, Regular, #8E8E93, letterSpacing -0.15sp
        Text(
            text = "즐겨찾기한 지역의 실시간 수온 정보",
            fontSize = 14.sp,
            color = TextGray,
            letterSpacing = (-0.15).sp
        )
    }

    // 하단 구분선 — Figma: #E5E5EA 0.5dp
    HorizontalDivider(
        thickness = 0.5.dp,
        color = HeaderDividerColor
    )
}

// ── 지역 추가/관리 버튼 ───────────────────────────────────────────────────────

/**
 * 지역 추가/관리 버튼.
 *
 * Figma: 흰 카드, radius 16dp, 높이 56dp, 그림자 rgba(0,0,0,0.04)
 *        ic_plus 아이콘 + "지역 추가/관리" (16sp, Medium, #2563EB)
 * iOS의 addRegionButton에 대응합니다.
 */
@Composable
private fun AddRegionButton(onClick: () -> Unit) {
    // [개념] remember { MutableInteractionSource() }는 리플 없는 클릭 처리에 사용됩니다.
    //        indication = null 로 클릭 시 물결 효과(Ripple)를 제거합니다.
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
            // Figma: 16sp, Medium, #2563EB, letterSpacing -0.31sp
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

/**
 * 즐겨찾기한 지역이 없을 때 표시되는 카드.
 *
 * Figma: 흰 카드, radius 16dp, 그림자 rgba(0,0,0,0.04)
 *        ic_star_empty 48dp + 텍스트 2줄 (중앙 정렬)
 * iOS의 emptyStateView에 대응합니다.
 */
@Composable
private fun EmptyStateView() {
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 빈 별 아이콘 — Figma: 48dp, tint #E5E5EA
            Icon(
                painter = painterResource(id = R.drawable.ic_star_empty),
                contentDescription = null,
                tint = Color(0xFFE5E5EA),
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 주 메시지 — Figma: 16sp, Regular, #8E8E93
            Text(
                text = "즐겨찾기한 지역이 없습니다",
                fontSize = 16.sp,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 보조 메시지 — Figma: 14sp, Regular, #AEAEB2
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
 * 즐겨찾기 지역의 수온 정보를 표시하는 카드.
 *
 * Figma: 높이 200dp, radius 16dp, 그림자 0 2dp 10dp rgba(0,0,0,0.08)
 *        배경: 그라디언트 (#4A7C9E → #6A9CBE), 배경 이미지는 구현 제외
 *        좌측: 표층(48sp Bold) + 중층/저층 / 우측: 지역명(18sp Bold) + 해역명(14sp)
 * iOS의 OceanRegionCardView.swift에 대응합니다.
 *
 * @param station 표시할 관측소 수온 데이터
 */
@Composable
private fun OceanRegionCardView(station: CombinedCurrentTemperature) {
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
            .background(brush = CardGradient)
    ) {
        // 콘텐츠 레이어
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
                // 상단: 표층 라벨 + 표층 온도
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // "표층" 라벨 — 14sp, white 80%
                    Text(
                        text = "표층",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        letterSpacing = (-0.15).sp
                    )
                    // 표층 온도 — 48sp Bold, white, 텍스트 그림자
                    // [개념] if/else 표현식은 값을 반환합니다. (Swift의 조건 표현식과 동일)
                    val surText = if (station.surTemperature.isNotEmpty()) "${station.surTemperature}°" else "-°"
                    Text(
                        text = surText,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.35.sp
                    )
                }

                // 하단: 중층/저층 보조 수온
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SubTemperatureRow(label = "중층", value = station.midTemperature)
                    SubTemperatureRow(label = "저층", value = station.botTemperature)
                }
            }

            // ── 우측: 지역명 + 해역명 ─────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // 지역명 — 18sp Bold, white
                Text(
                    text = station.stationName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.44).sp
                )
                // 해역명 — 14sp Medium, white 90%
                Text(
                    text = station.seaName.ifEmpty { resolveSeaName(station.stationName) },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = (-0.15).sp
                )
            }
        }
    }
}

/**
 * 중층/저층 보조 수온 행.
 *
 * 값이 비어있거나 "0"이면 "데이터 없음"으로 표시합니다.
 * iOS의 subTemperatureSection 로직에 대응합니다.
 *
 * @param label "중층" 또는 "저층"
 * @param value 수온 문자열
 */
@Composable
private fun SubTemperatureRow(label: String, value: String) {
    // [개념] isEmpty() || == "0" 으로 유효하지 않은 데이터를 걸러냅니다.
    val isEmpty = value.isEmpty() || value == "0"

    if (isEmpty) {
        // 데이터 없음 — 14sp, white 50%
        Text(
            text = "$label: 데이터 없음",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            letterSpacing = (-0.15).sp
        )
    } else {
        // 값 있음 — "중층: " (18sp, white 95%) + 온도값 (18sp SemiBold, white 95%)
        Row {
            Text(
                text = "$label: ",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = (-0.44).sp
            )
            Text(
                text = "$value°",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = (-0.44).sp
            )
        }
    }
}

/**
 * 관측소명에서 해역명(서해/남해/동해)을 추정합니다.
 * seaName 필드가 비어 있을 때 폴백으로 사용합니다.
 * iOS의 seaRegionName 연산 프로퍼티에 대응합니다.
 *
 * @param stationName 관측소 한글명
 * @return 해역명 문자열 ("서해", "남해", "동해", "해역" 중 하나)
 */
private fun resolveSeaName(stationName: String): String {
    val westSeaKeywords = listOf("서산", "목포", "군산", "인천", "태안", "보령", "부안", "영광", "무안", "신안", "백령도", "서천")
    val southSeaKeywords = listOf("여수", "통영", "거제", "남해", "완도", "고흥", "진도", "해남", "장흥", "강진", "사천", "서제주", "제주", "추자도", "보성", "진해")
    val eastSeaKeywords = listOf("울산", "포항", "동해", "강릉", "속초", "삼척", "울진", "영덕", "경주", "부산", "양양", "고성", "기장", "나곡", "덕천", "온양", "진하", "구룡포", "고리")

    // [개념] any { }는 조건을 만족하는 요소가 하나라도 있으면 true를 반환합니다.
    //        Swift의 contains(where:)와 동일합니다.
    if (westSeaKeywords.any { stationName.contains(it) }) return "서해"
    if (southSeaKeywords.any { stationName.contains(it) }) return "남해"
    if (eastSeaKeywords.any { stationName.contains(it) }) return "동해"
    return "해역"
}
