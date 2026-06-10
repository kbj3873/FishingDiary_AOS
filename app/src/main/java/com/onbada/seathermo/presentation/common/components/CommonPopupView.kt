package com.onbada.seathermo.presentation.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ── 팝업 레이아웃 타입 ────────────────────────────────────────────────────────

/**
 * 팝업 버튼 배치 방향.
 *
 * iOS의 PopupLayoutType enum에 대응합니다.
 * - [Horizontal]: 버튼 좌우 배치 (기본 스타일)
 * - [Vertical]: 버튼 상하 배치 (설정 이동 등 긴 텍스트 버튼 사용 시)
 */
enum class PopupLayoutType {
    Horizontal, // 버튼 좌우 배치
    Vertical    // 버튼 상하 배치
}

// ── 내부 색상 상수 ────────────────────────────────────────────────────────────
private val PopupBackground = Color(0xF2FFFFFF)      // white opacity 0.95
private val PopupDividerColor = Color(0x1A000000)    // black opacity 0.1
private val PopupTitleColor = Color.Black
private val PopupMessageColor = Color(0xFF8E8E93)    // System Gray
private val PopupOptionColor = Color(0xFF6B7280)     // Gray-500
private val PopupPrimaryDefault = Color(0xFF2563EB)  // Blue
val PopupDestructiveColor = Color(0xFFFF3B30)        // Red (외부 사용 가능)
private val DimBackground = Color(0x66000000)        // black opacity 0.4

/**
 * 앱 전역 공용 팝업 컴포넌트.
 *
 * [개념] Dialog()는 현재 화면 위에 떠 있는 팝업 레이어를 만듭니다.
 *        iOS의 CommonPopupView.swift에 대응합니다.
 *
 * ## 사용 예시
 * ```kotlin
 * // 단일 버튼 (네트워크 오류)
 * CommonPopupView(
 *     title = "네트워크 오류",
 *     message = "네트워크 연결 상태를 확인해주세요.",
 *     primaryButtonText = "확인",
 *     onPrimaryClick = { viewModel.dismissError() }
 * )
 *
 * // 2버튼 Destructive (삭제 확인)
 * CommonPopupView(
 *     title = "낚시 기록 삭제",
 *     message = "이 기록을 삭제하시겠습니까?",
 *     primaryButtonText = "삭제",
 *     primaryButtonColor = PopupDestructiveColor,
 *     primaryButtonWeight = FontWeight.Normal,
 *     onPrimaryClick = { viewModel.delete() },
 *     secondaryButtonText = "취소",
 *     onSecondaryClick = { showDialog = false }
 * )
 * ```
 *
 * @param title 팝업 제목 (17sp, SemiBold)
 * @param message 본문 메시지 (17sp, Regular, #8E8E93)
 * @param layoutType 버튼 배치 방향 (기본: [PopupLayoutType.Horizontal])
 * @param primaryButtonText 주 버튼 텍스트 (기본: "확인")
 * @param primaryButtonColor 주 버튼 텍스트 색상 (기본: #2563EB)
 * @param primaryButtonWeight 주 버튼 폰트 굵기 (기본: SemiBold)
 * @param onPrimaryClick 주 버튼 클릭 콜백
 * @param secondaryButtonText 보조 버튼 텍스트 (null이면 단일 버튼 스타일)
 * @param onSecondaryClick 보조 버튼 클릭 콜백
 * @param checkboxText 체크박스 라벨 (null이면 체크박스 미노출)
 * @param checkboxChecked 체크박스 선택 여부
 * @param onCheckboxCheckedChange 체크박스 선택 변경 콜백
 * @param onDismiss 팝업 바깥 탭 또는 뒤로가기 시 콜백 (기본: primaryAction과 동일)
 */
@Composable
fun CommonPopupView(
    title: String,
    message: String,
    layoutType: PopupLayoutType = PopupLayoutType.Horizontal,
    primaryButtonText: String = "확인",
    primaryButtonColor: Color = PopupPrimaryDefault,
    primaryButtonWeight: FontWeight = FontWeight.SemiBold,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    checkboxText: String? = null,
    checkboxChecked: Boolean = false,
    onCheckboxCheckedChange: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit = onPrimaryClick
) {
    // [개념] Dialog()는 시스템 레벨 오버레이 레이어를 생성합니다.
    //        usePlatformDefaultWidth = false 로 Compose가 직접 너비를 제어합니다.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // 팝업 카드 너비 — iOS 기준: horizontal 272dp, vertical 320dp
        val popupWidth: Dp = when (layoutType) {
            PopupLayoutType.Horizontal -> 272.dp
            PopupLayoutType.Vertical -> 320.dp
        }

        // 버튼 영역 높이 — iOS 기준: horizontal 44dp, vertical 50dp
        val buttonHeight: Dp = when (layoutType) {
            PopupLayoutType.Horizontal -> 44.dp
            PopupLayoutType.Vertical -> 50.dp
        }

        Box(
            modifier = Modifier
                .width(popupWidth)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0x4D000000),
                    spotColor = Color(0x4D000000)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(PopupBackground)
        ) {
            Column {
                // ── 1. 콘텐츠 영역 (제목 + 메시지) ──────────────────────────
                ContentArea(
                    title = title,
                    message = message,
                    checkboxText = checkboxText,
                    checkboxChecked = checkboxChecked,
                    onCheckboxCheckedChange = onCheckboxCheckedChange
                )

                // ── 2. 버튼 영역 ──────────────────────────────────────────────
                when (layoutType) {
                    PopupLayoutType.Horizontal -> HorizontalButtons(
                        buttonHeight = buttonHeight,
                        primaryText = primaryButtonText,
                        primaryColor = primaryButtonColor,
                        primaryWeight = primaryButtonWeight,
                        onPrimaryClick = onPrimaryClick,
                        secondaryText = secondaryButtonText,
                        onSecondaryClick = onSecondaryClick
                    )
                    PopupLayoutType.Vertical -> VerticalButtons(
                        buttonHeight = buttonHeight,
                        primaryText = primaryButtonText,
                        primaryColor = primaryButtonColor,
                        primaryWeight = primaryButtonWeight,
                        onPrimaryClick = onPrimaryClick,
                        secondaryText = secondaryButtonText,
                        onSecondaryClick = onSecondaryClick
                    )
                }
            }
        }
    }
}

// ── 콘텐츠 영역 ──────────────────────────────────────────────────────────────

@Composable
private fun ContentArea(
    title: String,
    message: String,
    checkboxText: String?,
    checkboxChecked: Boolean,
    onCheckboxCheckedChange: ((Boolean) -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 24.dp,
                bottom = if (checkboxText == null) 24.dp else 16.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 제목 — iOS: 17sp, SemiBold, Black
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = PopupTitleColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // 메시지 — iOS: 17sp, Regular, #8E8E93
        Text(
            text = message,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = PopupMessageColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (checkboxText != null && onCheckboxCheckedChange != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 8.dp, end = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onCheckboxCheckedChange(!checkboxChecked) }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checkboxChecked,
                    onCheckedChange = onCheckboxCheckedChange
                )
                Text(
                    text = checkboxText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = PopupOptionColor
                )
            }
        }
    }
}

// ── Horizontal 버튼 배치 (좌: Secondary, 우: Primary) ────────────────────────

@Composable
private fun HorizontalButtons(
    buttonHeight: Dp,
    primaryText: String,
    primaryColor: Color,
    primaryWeight: FontWeight,
    onPrimaryClick: () -> Unit,
    secondaryText: String?,
    onSecondaryClick: (() -> Unit)?
) {
    // [개념] IntrinsicSize.Min은 Column 내부 최소 높이에 맞춥니다.
    //        VerticalDivider가 HStack 높이를 꽉 채우도록 합니다.
    Column {
        HorizontalDivider(color = PopupDividerColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 보조 버튼 (왼쪽) — secondaryText가 있을 때만 표시
            if (secondaryText != null) {
                PopupButton(
                    text = secondaryText,
                    color = PopupTitleColor,
                    weight = FontWeight.Normal,
                    height = buttonHeight,
                    onClick = { onSecondaryClick?.invoke() },
                    modifier = Modifier.weight(1f)
                )
                // 세로 구분선
                VerticalDivider(color = PopupDividerColor)
            }

            // 주 버튼 (오른쪽 또는 전체)
            PopupButton(
                text = primaryText,
                color = primaryColor,
                weight = primaryWeight,
                height = buttonHeight,
                onClick = onPrimaryClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Vertical 버튼 배치 (위: Secondary, 아래: Primary) ────────────────────────

@Composable
private fun VerticalButtons(
    buttonHeight: Dp,
    primaryText: String,
    primaryColor: Color,
    primaryWeight: FontWeight,
    onPrimaryClick: () -> Unit,
    secondaryText: String?,
    onSecondaryClick: (() -> Unit)?
) {
    Column {
        // 보조 버튼 (위) — secondaryText가 있을 때만 표시
        if (secondaryText != null) {
            HorizontalDivider(color = PopupDividerColor)
            PopupButton(
                text = secondaryText,
                color = PopupTitleColor,
                weight = FontWeight.Normal,
                height = buttonHeight,
                onClick = { onSecondaryClick?.invoke() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 주 버튼 (아래)
        HorizontalDivider(color = PopupDividerColor)
        PopupButton(
            text = primaryText,
            color = primaryColor,
            weight = primaryWeight,
            height = buttonHeight,
            onClick = onPrimaryClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── 버튼 공통 컴포넌트 ────────────────────────────────────────────────────────

@Composable
private fun PopupButton(
    text: String,
    color: Color,
    weight: FontWeight,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .height(height)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = weight,
            color = color
        )
    }
}

// ── 딤 오버레이 래퍼 ─────────────────────────────────────────────────────────

/**
 * 팝업을 딤 오버레이와 함께 표시하는 래퍼.
 *
 * [개념] ZStack 구조로 딤 배경 위에 [CommonPopupView]를 올립니다.
 *        iOS의 ZStack { Color.black.opacity(0.4) ... CommonPopupView(...) }에 대응합니다.
 *
 * ViewModel의 showErrorAlert 같은 Boolean 상태로 조건부 호출합니다:
 * ```kotlin
 * if (uiState.showErrorAlert) {
 *     CommonPopupOverlay(
 *         title = "네트워크 오류",
 *         message = uiState.errorMessage ?: "알 수 없는 오류",
 *         onPrimaryClick = { viewModel.dismissError() }
 *     )
 * }
 * ```
 */
@Composable
fun CommonPopupOverlay(
    title: String,
    message: String,
    layoutType: PopupLayoutType = PopupLayoutType.Horizontal,
    primaryButtonText: String = "확인",
    primaryButtonColor: Color = PopupPrimaryDefault,
    primaryButtonWeight: FontWeight = FontWeight.SemiBold,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    checkboxText: String? = null,
    checkboxChecked: Boolean = false,
    onCheckboxCheckedChange: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit = onPrimaryClick
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DimBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        // 팝업 자체 클릭이 바깥 딤 탭으로 전파되지 않도록 clickable 차단
        Box(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
        ) {
            CommonPopupView(
                title = title,
                message = message,
                layoutType = layoutType,
                primaryButtonText = primaryButtonText,
                primaryButtonColor = primaryButtonColor,
                primaryButtonWeight = primaryButtonWeight,
                onPrimaryClick = onPrimaryClick,
                secondaryButtonText = secondaryButtonText,
                onSecondaryClick = onSecondaryClick,
                checkboxText = checkboxText,
                checkboxChecked = checkboxChecked,
                onCheckboxCheckedChange = onCheckboxCheckedChange,
                onDismiss = onDismiss
            )
        }
    }
}
