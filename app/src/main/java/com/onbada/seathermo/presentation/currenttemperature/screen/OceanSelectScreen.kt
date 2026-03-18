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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.onbada.seathermo.presentation.currenttemperature.viewmodel.OceanSelectViewModel

// ── 색상 상수 ────────────────────────────────────────────────────────────────
// Figma 기준: 시트 배경 = 흰색, 셀 배경 = #F2F2F7
private val SheetBackground = Color.White
private val CardBackground = Color(0xFFF2F2F7)
private val AccentBlue = Color(0xFF2563EB)
private val TextBlack = Color.Black
private val TextGray = Color(0xFF8E8E93)
private val BadgeBackground = Color(0xFFE5E5EA)

/**
 * 지역 선택(즐겨찾기 관리) BottomSheet.
 *
 * [개념] ModalBottomSheet는 iOS의 .sheet(presentationDetents: .fraction(0.8))에 대응합니다.
 *        화면 아래에서 올라오는 패널 형태의 오버레이입니다.
 *
 * iOS의 OceanSelectView.swift에 대응합니다.
 *
 * @param viewModel 관측소 목록과 즐겨찾기 상태를 관리하는 ViewModel
 * @param onDismiss Sheet를 닫을 때 호출 (완료 버튼 또는 외부 탭)
 * @param onDataUpdated 즐겨찾기 변경 시 상위 화면에 알리는 콜백
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OceanSelectScreen(
    viewModel: OceanSelectViewModel,
    onDismiss: () -> Unit,
    onDataUpdated: () -> Unit
) {
    // [개념] rememberModalBottomSheetState()는 BottomSheet의 확장/축소 상태를 관리합니다.
    //        skipPartiallyExpanded = true 로 절반만 열리는 상태를 건너뜁니다.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val oceanStations by viewModel.oceanStations.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // [개념] DisposableEffect는 Composable이 화면을 떠날 때(onDispose) 정리 작업을 실행합니다.
    //        iOS의 .onDisappear { viewModel.viewDidDisappear() }에 대응합니다.
    //        viewModel.onDataUpdated 콜백을 연결하여 즐겨찾기 변경 시 상위 ViewModel에 전파합니다.
    DisposableEffect(viewModel) {
        viewModel.onDataUpdated = { onDataUpdated() }
        onDispose {
            viewModel.onViewDisappear()
            viewModel.onDataUpdated = null
        }
    }

    // [개념] ModalBottomSheet는 Material3 컴포넌트로, onDismissRequest로 바깥 탭 시 닫힘을 처리합니다.
    //        dragHandle 파라미터로 기본 핸들을 교체하여 Figma 수치(바 위 8dp, 높이 5dp, 아래 12dp)를 정확히 구현합니다.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.8f),
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
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 헤더 ──────────────────────────────────────────────────────
            HeaderSection(onDismiss = onDismiss)

            // ── 관측소 목록 ───────────────────────────────────────────────
            if (isLoading) {
                LoadingView()
            } else {
                StationList(
                    stations = oceanStations,
                    onToggle = { station ->
                        viewModel.toggleSelection(station, !station.isChecked)
                    }
                )
            }
        }
    }
}

// ── 헤더 ─────────────────────────────────────────────────────────────────────

/**
 * 지역 선택 헤더.
 *
 * iOS: HStack(제목+부제목 VStack, Spacer, 완료 Button)
 * Figma: dragHandle(25dp) 바로 아래 시작 — top padding 없음, horizontal 20dp, bottom 20dp
 */
@Composable
private fun HeaderSection(onDismiss: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    // Figma: 헤더 섹션 top = 25dp (drag handle 25dp 후 바로 시작)
    // dragHandle 컴포저블이 이미 8+5+12=25dp를 차지하므로 여기서는 top padding 불필요
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // 제목 + 부제목
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // 제목: iOS 28sp, Bold, Black
            Text(
                text = "지역 선택",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            // 부제목: iOS 14sp, Regular, #8E8E93
            Text(
                text = "즐겨찾기에 추가할 지역을 선택하세요",
                fontSize = 14.sp,
                color = TextGray
            )
        }

        // 완료 버튼: iOS 16sp, Bold, #2563EB
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
                .padding(4.dp) // 탭 영역 확보
        )
    }
}

// ── 관측소 목록 ───────────────────────────────────────────────────────────────

/**
 * 관측소 리스트.
 *
 * [개념] LazyColumn은 화면에 보이는 항목만 렌더링하는 성능 최적화 리스트입니다.
 *        iOS의 LazyVStack + ForEach에 대응합니다.
 */
@Composable
private fun StationList(
    stations: List<CombinedCurrentTemperature>,
    onToggle: (CombinedCurrentTemperature) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 10.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // [개념] key = { it.stationCode }로 고유 키를 지정하면 리스트 항목 추가/삭제 시
        //        불필요한 Recomposition을 방지합니다. Swift의 ForEach(id: \.self)에 대응합니다.
        items(items = stations, key = { it.stationCode }) { station ->
            StationItem(
                station = station,
                onToggle = { onToggle(station) }
            )
        }
        // 하단 여백 (iOS padding bottom 30dp)
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ── 관측소 아이템 카드 ────────────────────────────────────────────────────────

/**
 * 관측소 목록의 개별 아이템 카드.
 *
 * Figma/iOS:
 * - 높이 60dp, 흰색 카드, radius 16dp
 * - 왼쪽: 지역명(17sp, Bold) + 해역 배지(11sp, SemiBold, #8E8E93, 배경 #E5E5EA, radius 4dp)
 * - 오른쪽: ic_star_on(선택) / ic_star_off(미선택), 24dp
 * - 탭 시 scale 0.98 (iOS ScaleButtonStyle) → Compose에서 animateFloatAsState로 구현
 */
@Composable
private fun StationItem(
    station: CombinedCurrentTemperature,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

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
        //        이를 제거해야 Row의 CenterVertically가 시각적으로도 정확하게 동작합니다.
        Text(
            text = station.stationName,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextBlack,
            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
        )

        if (station.seaName.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            SeaNameBadge(seaName = station.seaName)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 즐겨찾기 아이콘 (on/off)
        // [개념] painterResource로 PNG 에셋을 불러옵니다.
        //        tint = Color.Unspecified 로 원본 색상(노란 별, 회색 별)을 그대로 사용합니다.
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

// ── 해역 배지 ────────────────────────────────────────────────────────────────

/**
 * 해역명 배지 (서해 / 동해 / 남해 / 제주).
 *
 * Figma/iOS: 11sp, SemiBold, #8E8E93, 배경 #E5E5EA, radius 4dp, padding H 6dp V 2dp
 */
@Composable
private fun SeaNameBadge(seaName: String) {
    // 배지: Box로 완전 분리하여 height=20dp 고정, 텍스트는 Box 안에서 수직 중앙 정렬
    // 지역명 Text에 includeFontPadding=false를 적용했으므로 Row CenterVertically가 시각적으로도 정확하게 동작
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(BadgeBackground)
            .padding(horizontal = 6.dp)
    ) {
        Text(
            text = seaName,
            fontSize = 11.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextGray
        )
    }
}

// ── 로딩 ─────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AccentBlue)
    }
}
