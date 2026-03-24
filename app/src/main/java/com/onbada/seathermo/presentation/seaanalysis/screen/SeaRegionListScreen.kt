package com.onbada.seathermo.presentation.seaanalysis.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onbada.seathermo.R
import com.onbada.seathermo.application.di.ApplicationDIContainer
import com.onbada.seathermo.domain.entity.Region
import com.onbada.seathermo.presentation.seaanalysis.viewmodel.SeaRegionListViewModel

// ── 색상 상수 ──────────────────────────────────────────────────────────────────
// Figma TemperatureRegionModal 기준
private val SheetBackground = Color.White
private val RowBackground = Color(0xFFF2F2F7)   // Figma: #F2F2F7
private val AccentBlue = Color(0xFF2563EB)       // Figma: #2563EB (닫기 버튼)
private val TitleColor = Color.Black
private val SubtitleColor = Color(0xFF8E8E93)    // Figma: #8E8E93
private val ChevronColor = Color(0xFFC7C7CC)     // Figma: #C7C7CC (chevron 아이콘)

/**
 * 해역별 관측소 목록을 표시하는 BottomSheet.
 *
 * [개념] ModalBottomSheet는 iOS의 `.sheet { SeaRegionListView(sea:) }`에 대응합니다.
 *        화면 아래에서 올라오는 패널 형태의 오버레이입니다.
 *
 * iOS의 SeaRegionListView.swift에 대응합니다.
 *
 * @param seaAreaId 표시할 해역 코드 ("W" 서해 / "E" 동해 / "S" 남해)
 * @param diContainer DI 컨테이너 (ViewModel factory 제공)
 * @param onDismiss Sheet를 닫을 때 호출
 * @param onRegionSelected 관측소 선택 시 호출 (SeaAnalysisDetailScreen 이동 용도)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeaRegionListScreen(
    seaAreaId: String,
    diContainer: ApplicationDIContainer,
    onDismiss: () -> Unit,
    onRegionSelected: (Region) -> Unit
) {
    // [개념] viewModel(factory = ...)은 커스텀 Factory로 ViewModel을 생성합니다.
    //        seaAreaId가 바뀌면 다른 ViewModel 인스턴스가 필요하므로
    //        key = seaAreaId 를 지정하여 해역이 바뀔 때 ViewModel을 재생성합니다.
    val viewModel: SeaRegionListViewModel = viewModel(
        key = seaAreaId,
        factory = diContainer.makeSeaRegionListViewModelFactory(seaAreaId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // [개념] rememberModalBottomSheetState()는 BottomSheet의 펼침/접힘 상태를 관리합니다.
    //        skipPartiallyExpanded = true 로 절반만 열리는 상태를 건너뛰어 완전히 펼쳐집니다.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // [개념] ModalBottomSheet는 onDismissRequest로 외부 탭/스와이프 다운 시 닫힘을 처리합니다.
    //        dragHandle 파라미터로 기본 핸들을 교체하여 Figma 수치를 정확히 구현합니다.
    // Material3 1.3.x 변경: fillMaxHeight를 ModalBottomSheet modifier에 적용하면 시트가 상단 기준으로 배치됨.
    // 대신 내부 Column에 화면 높이의 90%를 직접 지정하여 시트가 하단에서 올라오도록 합니다.
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
            .height(screenHeight * 0.9f)
        ) {

            // ── 헤더 ──────────────────────────────────────────────────────────
            // iOS: VStack(spacing: 0) { headerSection.padding(.top, 30).padding(.horizontal, 16).padding(.bottom, 20) }
            // Figma: dragHandle(25dp) 이후 top=25dp, horizontal=16dp, bottom=20dp
            RegionListHeader(
                title = uiState.title,
                subtitle = uiState.subtitle,
                onDismiss = onDismiss
            )

            // ── 관측소 목록 ───────────────────────────────────────────────────
            // iOS: ScrollView { LazyVStack(spacing: 12) { ... }.padding(.horizontal, 16).padding(.top, 12).padding(.bottom, 32) }
            RegionList(
                regions = uiState.observatories,
                onRegionSelected = { region ->
                    onRegionSelected(region)
                    onDismiss()
                }
            )
        }
    }
}

// ── 헤더 섹션 ────────────────────────────────────────────────────────────────

/**
 * 해역명 + 부제목 + 닫기 버튼으로 구성된 헤더.
 *
 * iOS: HStack(alignment: .top) { VStack(leading, spacing: 4) { 제목, 부제목 }, Spacer, 닫기 버튼 }
 * Figma: top=25dp, horizontal=16dp, bottom=20dp
 */
@Composable
private fun RegionListHeader(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    // [개념] MutableInteractionSource를 직접 생성하고 indication = null 을 지정하면
    //        클릭 시 ripple 효과 없이 탭 이벤트만 처리합니다. iOS의 PlainButtonStyle()과 동일합니다.
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // dragHandle 컴포저블이 이미 8+5+12=25dp를 차지하므로 여기서는 top padding 불필요
            .padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // 제목 + 부제목 (좌측)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Figma: 28sp Bold Black, tracking -0.12sp
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TitleColor,
                letterSpacing = (-0.12).sp
            )
            // Figma: 15sp Medium #8E8E93, tracking -0.23sp
            Text(
                text = subtitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = SubtitleColor,
                letterSpacing = (-0.23).sp
            )
        }

        // 닫기 버튼 (우측)
        // Figma: 17sp SemiBold #2563EB, tracking -0.43sp
        Text(
            text = "닫기",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentBlue,
            letterSpacing = (-0.43).sp,
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
 * 관측소 목록 LazyColumn.
 *
 * iOS: ScrollView { LazyVStack(spacing: 12) { ForEach(viewModel.observatories) { SeaRegionRowView } } }
 * Figma: horizontal 16dp, top 12dp, bottom 32dp, 행 간격 12dp
 */
@Composable
private fun RegionList(
    regions: List<Region>,
    onRegionSelected: (Region) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // [개념] key = { it.regionCode }로 각 아이템에 안정적인 고유 키를 부여합니다.
        //        리스트가 변경될 때 불필요한 Recomposition을 방지합니다.
        //        iOS의 ForEach(viewModel.observatories, id: \.id)에 대응합니다.
        items(items = regions, key = { it.regionCode }) { region ->
            SeaRegionRowView(
                regionName = region.regionName,
                onClick = { onRegionSelected(region) }
            )
        }
        // iOS: padding bottom 32dp 대응
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

// ── 관측소 행 아이템 ───────────────────────────────────────────────────────────

/**
 * 관측소 목록의 개별 행 아이템.
 *
 * iOS: SeaRegionRowView.swift
 * - HStack { 지역명(16sp SemiBold), Spacer, chevron.right(14sp Medium #C7C7CC) }
 * - height: 62dp, background: #F2F2F7, cornerRadius: 16dp, padding horizontal: 16dp
 *
 * Figma: Button height=62dp, background=#F2F2F7, radius=16dp
 *        내부: Container(horizontal 16dp) → 좌: 지역명 / 우: Icon 20x20dp
 */
@Composable
private fun SeaRegionRowView(
    regionName: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(RowBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 지역명: Figma 16sp SemiBold Black, tracking -0.31sp
        Text(
            text = regionName,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TitleColor,
            letterSpacing = (-0.31).sp
        )

        // Chevron 아이콘: Figma 20x20dp, #C7C7CC
        // [개념] tint = Color.Unspecified 로 PNG 원본 색상(#C7C7CC)을 그대로 사용합니다.
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
    }
}
