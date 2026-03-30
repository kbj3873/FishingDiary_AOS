package com.onbada.seathermo.presentation.setting.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onbada.seathermo.domain.entity.MapType
import com.onbada.seathermo.infrastructure.webview.WebPage
import com.onbada.seathermo.presentation.common.components.CommonPopupOverlay
import com.onbada.seathermo.presentation.common.components.PopupLayoutType
import com.onbada.seathermo.presentation.setting.viewmodel.SettingViewModel

// ── 색상 상수 ─────────────────────────────────────────────

// Figma: 배경 #F2F2F7
private val SettingBackground = Color(0xFFF2F2F7)

// Figma: 섹션 헤더 #8E8E93
private val SectionHeaderColor = Color(0xFF8E8E93)

// Figma: 행 아이콘 #8E8E93
private val RowIconColor = Color(0xFF8E8E93)

// Figma: 행 구분선 rgba(198,198,200,0.3)
private val RowDividerColor = Color(0x4DC6C6C8)

// Figma: 지도 선택 체크마크 #2563EB
private val CheckmarkColor = Color(0xFF2563EB)

// Figma: 우측 상세 텍스트(버전 등) #8E8E93
private val DetailTextColor = Color(0xFF8E8E93)

// Figma: 저작권 텍스트 #C7C7CC
private val CopyrightTextColor = Color(0xFFC7C7CC)

// Figma: 헤더 구분선 rgba(0,0,0,0.1)
private val HeaderDividerColor = Color(0x1A000000)

/**
 * 설정 메인 화면.
 *
 * [개념] @Composable 어노테이션이 붙은 함수는 UI를 '선언'합니다.
 *        ViewModel의 상태(state)가 변경되면 Compose 런타임이 자동으로 재호출(Recomposition)합니다.
 *        iOS의 SettingView.swift에 대응합니다.
 */
@Composable
fun SettingScreen(
    viewModel: SettingViewModel,
    onNavigateToWebPage: (WebPage) -> Unit = {}
) {

    // [개념] collectAsStateWithLifecycle()은 StateFlow를 Compose State로 변환합니다.
    //        화면이 백그라운드로 가면 수집을 자동 중단하여 불필요한 연산을 막습니다.
    //        Swift의 @StateObject + @Published 조합과 동일한 역할입니다.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 낚시 기록 중 지도 변경 차단 팝업
    // [개념] CommonPopupOverlay는 딤 배경 + 팝업 카드를 한 번에 처리합니다.
    //        iOS의 CommonPopupView(layoutType: .horizontal)에 대응합니다.
    if (uiState.showRecordingBlockedDialog) {
        CommonPopupOverlay(
            title = "지도 변경 불가",
            message = "낚시 기록 중에는 지도 타입을 변경할 수 없습니다.\n기록을 종료한 후 변경해 주세요.",
            layoutType = PopupLayoutType.Horizontal,
            primaryButtonText = "확인",
            onPrimaryClick = { viewModel.dismissRecordingBlockedDialog() },
            onDismiss = { viewModel.dismissRecordingBlockedDialog() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingBackground)
    ) {
        // ── 헤더 ─────────────────────────────────────────
        SettingHeader()

        // ── 스크롤 콘텐츠 ──────────────────────────────────
        // [개념] verticalScroll(rememberScrollState())은 Column을 스크롤 가능하게 만듭니다.
        //        iOS의 ScrollView에 대응합니다.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 일반 섹션 (공지사항)
            SettingNoticeSection(
                onNoticeClick = { onNavigateToWebPage(WebPage.NOTICES) }
            )

            // 지도 섹션 (구글맵 / 카카오맵 선택)
            SettingMapSection(
                selectedMapType = uiState.selectedMapType,
                onMapTypeSelected = { viewModel.updateMapType(it) }
            )

            // 정보 섹션 (앱 사용 가이드, 앱 정보, 오픈소스 라이선스)
            SettingInfoSection(
                appVersion = uiState.appVersion,
                onAppInfoClick = { /* TODO: 앱 정보 화면 진입 */ },
                onLicenseClick = { onNavigateToWebPage(WebPage.LICENSES) }
            )

            // 저작권 푸터
            SettingCopyrightSection()
        }
    }
}

// ── 헤더 ─────────────────────────────────────────────────

/**
 * 설정 화면 상단 헤더.
 *
 * [개념] iOS NavigationStack의 large title 스타일을 재현합니다.
 *        Figma: "설정" 30sp Bold, 헤더 배경 흰색, 하단 구분선.
 */
@Composable
private fun SettingHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Text(
            text = "설정",
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.35).sp,
                color = Color.Black
            ),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 16.dp)
        )
        // Figma: border-b 1px rgba(0,0,0,0.1)
        HorizontalDivider(
            thickness = 0.5.dp,
            color = HeaderDividerColor
        )
    }
}

// ── 일반 섹션 ─────────────────────────────────────────────

/**
 * 일반 섹션: 공지사항 1개 행.
 */
@Composable
private fun SettingNoticeSection(
    onNoticeClick: () -> Unit
) {
    SettingSection(title = "일반") {
        SettingRowWithChevron(
            icon = Icons.Outlined.Notifications,
            title = "공지사항",
            isLast = true,
            onClick = onNoticeClick
        )
    }
}

// ── 지도 섹션 ─────────────────────────────────────────────

/**
 * 지도 섹션: 구글 지도 / 카카오맵 선택 행.
 *
 * [개념] iOS의 애플 지도(AppleMap) → Android에서는 구글 지도(GOOGLE_MAP)로 대응합니다.
 *        선택된 지도 타입에는 체크마크(✓)를 표시합니다.
 */
@Composable
private fun SettingMapSection(
    selectedMapType: MapType,
    onMapTypeSelected: (MapType) -> Unit
) {
    SettingSection(title = "지도") {
        SettingMapRow(
            icon = Icons.Outlined.Map,
            title = "구글 지도",
            isSelected = selectedMapType == MapType.GOOGLE_MAP,
            isLast = false,
            onClick = { onMapTypeSelected(MapType.GOOGLE_MAP) }
        )
        SettingMapRow(
            icon = Icons.Outlined.Map,
            title = "카카오맵",
            isSelected = selectedMapType == MapType.KAKAO_MAP,
            isLast = true,
            onClick = { onMapTypeSelected(MapType.KAKAO_MAP) }
        )
    }
}

// ── 정보 섹션 ─────────────────────────────────────────────

/**
 * 정보 섹션: 앱 사용 가이드, 앱 정보(버전), 오픈소스 라이선스.
 */
@Composable
private fun SettingInfoSection(
    appVersion: String,
    onAppInfoClick: () -> Unit,
    onLicenseClick: () -> Unit
) {
    SettingSection(title = "정보") {
        SettingRowWithChevron(
            icon = Icons.Outlined.Info,
            title = "앱 정보",
            detail = "버전 $appVersion",
            isLast = false,
            onClick = onAppInfoClick
        )
        SettingRowWithChevron(
            icon = Icons.Outlined.Description,
            title = "오픈소스 라이선스",
            isLast = true,
            onClick = onLicenseClick
        )
    }
}

// ── 저작권 푸터 ────────────────────────────────────────────

/**
 * 화면 하단 저작권 문구 섹션.
 *
 * Figma: "© 2026 바다 현장정보 시스템" / "All rights reserved" — 색상 #C7C7CC, 13sp.
 */
@Composable
private fun SettingCopyrightSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "© 2026 바다 현장정보 시스템",
            style = TextStyle(
                fontSize = 13.sp,
                color = CopyrightTextColor,
                letterSpacing = (-0.08).sp
            )
        )
        Text(
            text = "All rights reserved",
            style = TextStyle(
                fontSize = 13.sp,
                color = CopyrightTextColor
            )
        )
    }
}

// ── 공통 레이아웃 컴포넌트 ──────────────────────────────────

/**
 * 섹션 헤더(라벨) + 흰색 카드 그룹을 감싸는 컨테이너.
 *
 * [개념] Kotlin의 함수형 파라미터 content: @Composable () -> Unit 은
 *        Swift의 @ViewBuilder 클로저와 동일합니다.
 *        호출부에서 중괄호 블록으로 자식 Composable을 전달합니다.
 *
 * @param title 섹션 헤더 텍스트 (예: "일반", "지도", "정보")
 * @param content 카드 내부에 배치할 행 Composable들
 */
@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 섹션 헤더 텍스트
        // Figma: 13sp, SemiBold, #8E8E93, 좌측 패딩 16dp
        Text(
            text = title,
            style = TextStyle(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SectionHeaderColor,
                letterSpacing = (-0.08).sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // 흰색 카드 (Figma: rounded-[16px], 그림자 대신 얇은 테두리로 표현)
        // [개념] clip(RoundedCornerShape())은 자식 콘텐츠를 둥근 모서리로 잘라냅니다.
        //        iOS의 .cornerRadius(16)과 동일합니다.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            content()
        }
    }
}

/**
 * 우측에 ChevronRight 화살표가 있는 표준 설정 행.
 *
 * [개념] isLast 파라미터로 마지막 행 여부를 판단하여 구분선 표시를 제어합니다.
 *        iOS의 commonRow(isLast:) 함수에 대응합니다.
 *
 * @param icon 좌측 아이콘 (Material Icons ImageVector)
 * @param title 행 제목 텍스트
 * @param detail 우측 상세 텍스트 (선택, 예: "버전 1.0.0")
 * @param isLast 마지막 행 여부 (true면 구분선 생략)
 * @param onClick 클릭 이벤트 콜백
 */
@Composable
private fun SettingRowWithChevron(
    icon: ImageVector,
    title: String,
    detail: String? = null,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Column {
        // [개념] clickable Modifier는 View에 클릭 이벤트를 추가합니다.
        //        iOS의 Button { action } label: { }과 동일한 역할입니다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 좌측 아이콘
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RowIconColor,
                modifier = Modifier.size(20.dp)
            )

            // 행 제목
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 17.sp,
                    color = Color.Black,
                    letterSpacing = (-0.43).sp
                ),
                modifier = Modifier.weight(1f)
            )

            // 우측 상세 텍스트 (선택적 표시)
            // [개념] detail?.let { }은 null이 아닐 때만 블록을 실행합니다.
            //        Swift의 if let detail = detail { } 과 동일합니다.
            detail?.let {
                Text(
                    text = it,
                    style = TextStyle(
                        fontSize = 17.sp,
                        color = DetailTextColor,
                        letterSpacing = (-0.43).sp
                    )
                )
            }

            // ChevronRight 아이콘
            // Figma: 20×20, rgba(0,0,0,0.2) 색상
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ">",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(alpha = 0.2f)
                    )
                )
            }
        }

        // 구분선: 마지막 행이 아닐 때만 표시
        // Figma: 0.5dp, rgba(198,198,200, ~0.3), 좌측 52dp 들여쓰기
        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                thickness = 0.5.dp,
                color = RowDividerColor
            )
        }
    }
}

/**
 * 지도 선택 행 — 선택 시 체크마크(✓), 미선택 시 빈 칸.
 *
 * [개념] isSelected 파라미터에 따라 우측 UI가 달라지는 조건부 렌더링입니다.
 *        iOS의 mapSelectionRow(isSelected:) 함수에 대응합니다.
 *
 * @param icon 좌측 지도 아이콘
 * @param title 지도 이름 (예: "구글 지도", "카카오맵")
 * @param isSelected 현재 선택된 지도 타입 여부
 * @param isLast 마지막 행 여부
 * @param onClick 클릭 시 지도 타입 변경 콜백
 */
@Composable
private fun SettingMapRow(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RowIconColor,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = title,
                style = TextStyle(
                    fontSize = 17.sp,
                    color = Color.Black,
                    letterSpacing = (-0.43).sp
                ),
                modifier = Modifier.weight(1f)
            )

            // 선택 체크마크
            // [개념] if (isSelected) { }는 조건부 Composable 렌더링입니다.
            //        Swift의 if isSelected { checkmark view }와 동일합니다.
            if (isSelected) {
                Text(
                    text = "✓",
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CheckmarkColor
                    )
                )
            } else {
                // 체크마크 자리 유지를 위한 여백
                Spacer(modifier = Modifier.size(17.dp))
            }
        }

        if (!isLast) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 52.dp),
                thickness = 0.5.dp,
                color = RowDividerColor
            )
        }
    }
}
