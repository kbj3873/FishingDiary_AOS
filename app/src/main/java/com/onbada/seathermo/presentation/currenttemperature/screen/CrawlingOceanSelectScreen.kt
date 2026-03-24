package com.onbada.seathermo.presentation.currenttemperature.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onbada.seathermo.R
import com.onbada.seathermo.domain.entity.CombinedCurrentTemperature
import com.onbada.seathermo.presentation.common.components.CommonPopupView
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.CrawlingOceanSelectViewModel

// ── 색상 상수 ────────────────────────────────────────────────────────────────
// Figma 기준: 시트 배경 = 흰색, 셀 배경 = #F2F2F7 (연한 그레이)
// iOS 코드(.background(F2F2F7) on sheet, .background(white) on cell)는 Figma와 반대로 구현되어 있었음
private val SheetBackground = Color.White
private val CardBackground = Color(0xFFF2F2F7)
private val AccentBlue = Color(0xFF2563EB)
private val TextBlack = Color.Black
private val TextGray = Color(0xFF8E8E93)
private val TextLightGray = Color(0xFFAEAEB2)
private val BadgeBackground = Color(0xFFE5E5EA)

/**
 * 크롤링 기반 지역 선택(즐겨찾기 관리) BottomSheet.
 *
 * [개념] Crawling 버전은 ALL_REGION_LIST(로컬 저장 전체 지역)를 기반으로 목록을 표시하며,
 *        즐겨찾기 최대 7개 제한 로직이 포함됩니다.
 *        iOS의 CrawlingOceanSelectView.swift에 대응합니다.
 *
 * @param viewModel 크롤링 기반 지역 선택 ViewModel
 * @param onDismiss Sheet를 닫을 때 호출
 * @param onDataUpdated 즐겨찾기 변경 시 상위 화면에 알리는 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrawlingOceanSelectScreen(
    viewModel: CrawlingOceanSelectViewModel,
    onDismiss: () -> Unit,
    onDataUpdated: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val oceanStations by viewModel.oceanStations.collectAsStateWithLifecycle()
    val showMaxAlert by viewModel.showMaxAlert.collectAsStateWithLifecycle()

    // [개념] DisposableEffect는 Composable이 트리에서 제거될 때 onDispose 블록을 실행합니다.
    //        iOS의 .onDisappear { viewModel.viewDidDisappear() }에 대응합니다.
    DisposableEffect(viewModel) {
        viewModel.onDataUpdated = { onDataUpdated() }
        onDispose {
            viewModel.onViewDisappear()
            viewModel.onDataUpdated = null
        }
    }

    // [개념] iOS의 .presentationDetents([.fraction(0.8)])에 대응합니다.
    //        tonalElevation = 0.dp 로 Material3 색조 오버레이를 제거하여 containerColor를 정확히 표현합니다.
    //        dragHandle 파라미터로 기본 핸들을 교체하여 Figma 수치(바 위 8dp, 높이 5dp, 아래 12dp)를 정확히 구현합니다.
    //        Material3 1.3.x 변경: fillMaxHeight를 ModalBottomSheet modifier에 적용하면 시트가 상단 기준으로 배치됨.
    //        대신 내부 Column에 화면 높이의 80%를 직접 지정하여 시트가 하단에서 올라오도록 합니다.
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBackground,
        tonalElevation = 0.dp,
        dragHandle = {
            // Figma: 바 top=8dp, 너비 36dp, 높이 5dp, 색상 #C6C6C8, bottom gap 12dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFC6C6C8))
                )
            }
        }
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .height(screenHeight * 0.8f)
        ) {

            // ── 헤더 ──────────────────────────────────────────────────────
            CrawlingOceanSelectHeader(onDismiss = onDismiss)

            // ── 목록 or Empty State ──────────────────────────────────────
            if (oceanStations.isEmpty()) {
                // ALL_REGION_LIST를 불러올 수 없는 경우 (iOS emptyStateView에 대응)
                CrawlingEmptyStateView()
            } else {
                CrawlingStationList(
                    stations = oceanStations,
                    onToggle = { station ->
                        viewModel.toggleSelection(station, !station.isChecked)
                    }
                )
            }
        }
    }

    // ── 최대 즐겨찾기 초과 팝업 ─────────────────────────────────────────────
    // iOS의 .alert("즐겨찾기 초과", isPresented: $viewModel.showMaxAlert)에 대응합니다.
    // CommonPopupView를 활용하여 앱 디자인 통일성을 유지합니다.
    if (showMaxAlert) {
        CommonPopupView(
            title = "즐겨찾기 초과",
            message = "즐겨찾기는 최대 7개까지 추가할 수 있습니다.",
            primaryButtonText = "확인",
            onPrimaryClick = { viewModel.dismissMaxAlert() }
        )
    }
}

// ── 헤더 ─────────────────────────────────────────────────────────────────────

/**
 * 크롤링 지역 선택 헤더.
 *
 * iOS와의 차이: 부제목에 "(최대 7개)" 문구 추가
 */
@Composable
private fun CrawlingOceanSelectHeader(onDismiss: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // dragHandle 컴포저블이 이미 8+5+12=25dp를 차지하므로 여기서는 top padding 불필요
            .padding(top = 0.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 제목: 28sp, Bold, Black
            Text(
                text = "지역 선택",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            // 부제목 — iOS CrawlingOceanSelectView: "(최대 7개)" 포함
            Text(
                text = "즐겨찾기에 추가할 지역을 선택하세요 (최대 7개)",
                fontSize = 14.sp,
                color = TextGray
            )
        }

        // 완료 버튼: 16sp, Bold, #2563EB
        Text(
            text = "완료",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AccentBlue,
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                )
                .padding(4.dp)
        )
    }
}

// ── 관측소 목록 ───────────────────────────────────────────────────────────────

@Composable
private fun CrawlingStationList(
    stations: List<CombinedCurrentTemperature>,
    onToggle: (CombinedCurrentTemperature) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items = stations, key = { it.stationCode }) { station ->
            CrawlingStationItem(
                station = station,
                onToggle = { onToggle(station) }
            )
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ── 관측소 아이템 카드 ────────────────────────────────────────────────────────

@Composable
private fun CrawlingStationItem(
    station: CombinedCurrentTemperature,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Figma: shadow = 0px 0px 0px 0px rgba(0,0,0,0.04) → 사실상 그림자 없음
    // shadow() modifier 제거 후 clip + background 만 사용하여 flat 카드로 표현
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [개념] includeFontPadding = false 로 지역명 Text의 바운딩 박스를 실제 글리프에 맞게 줄입니다.
        //        기본값(true)일 때 바운딩 박스 위쪽에 여분 공간이 생겨 시각적 중심이 어긋납니다.
        Text(
            text = station.stationName,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )

        // 해역 배지 — Box로 완전 분리하여 height=20dp 고정, 텍스트는 Box 안에서 수직 중앙 정렬
        if (station.seaName.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(y = 1.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BadgeBackground)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = station.seaName,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextGray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 즐겨찾기 아이콘 — tint = Color.Unspecified 로 원본 PNG 색상 그대로 사용
        Icon(
            painter = painterResource(
                id = if (station.isChecked) R.drawable.ic_star_on else R.drawable.ic_star_off
            ),
            contentDescription = if (station.isChecked) "즐겨찾기 해제" else "즐겨찾기 추가",
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ── Empty State (관측소 목록 없음) ────────────────────────────────────────────

/**
 * ALL_REGION_LIST를 불러올 수 없는 예외 상황의 Empty State.
 *
 * iOS CrawlingOceanSelectView의 emptyStateView에 대응합니다.
 */
@Composable
private fun CrawlingEmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "관측소 목록을 불러올 수 없습니다",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextGray
            )
            Text(
                text = "앱을 재시작하거나 잠시 후 다시 시도해주세요",
                fontSize = 14.sp,
                color = TextLightGray
            )
        }
    }
}
