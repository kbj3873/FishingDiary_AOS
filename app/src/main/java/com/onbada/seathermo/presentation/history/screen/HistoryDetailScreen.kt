package com.onbada.seathermo.presentation.history.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.onbada.seathermo.R
import com.onbada.seathermo.application.di.ApplicationDIContainer
import com.onbada.seathermo.presentation.common.components.CommonPopupOverlay
import com.onbada.seathermo.presentation.common.components.PopupDestructiveColor
import com.onbada.seathermo.presentation.history.viewmodel.HistoryDetailViewModel
import com.onbada.seathermo.presentation.history.viewmodel.HistoryDetailUiState
import java.io.File

// ─── 색상 상수 (iOS/Figma 기준) ────────────────────────────────────────────────
private val PrimaryBlue = Color(0xFF2563EB)
private val TextBlack = Color(0xFF1C1C1E)
private val TextGray = Color(0xFF8E8E93)
private val WarningRed = Color(0xFFFF3B30)
private val DividerColor = Color(0xFFE5E5EA) // iOS Divider Color

/**
 * 히스토리 상세 화면.
 */
@Composable
fun HistoryDetailScreen(
    sessionId: String,
    diContainer: ApplicationDIContainer,
    onNavigateBack: (shouldRefresh: Boolean) -> Unit,
    onImageClick: (path: String) -> Unit = {}
) {
    val viewModel: HistoryDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = diContainer.makeHistoryDetailViewModelFactory(sessionId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDeletePopup by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchSessionData()
    }

    LaunchedEffect(uiState.shouldDismiss) {
        if (uiState.shouldDismiss) {
            onNavigateBack(true)
        }
    }

    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 지도 배경
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Google Map Placeholder", 
                textAlign = TextAlign.Center,
                color = TextGray
            )
        }

        // 2. 상단 통합 정보 카드 (iOS 스타일 고도화)
        HistoryInfoCard(
            uiState = uiState,
            isExpanded = isExpanded,
            onToggleExpand = { isExpanded = !isExpanded },
            onBackClick = { onNavigateBack(false) },
            onDeleteClick = { showDeletePopup = true },
            onImageClick = onImageClick,
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter)
        )

        // 3. 삭제 확인 팝업
        if (showDeletePopup) {
            CommonPopupOverlay(
                title = "낚시 기록 삭제",
                message = "이 낚시 기록을 삭제하시겠습니까? 삭제된 기록은 복구할 수 없습니다.",
                primaryButtonText = "삭제",
                primaryButtonColor = PopupDestructiveColor,
                onPrimaryClick = {
                    showDeletePopup = false
                    viewModel.deleteRecord()
                },
                secondaryButtonText = "취소",
                onSecondaryClick = { showDeletePopup = false },
                onDismiss = { showDeletePopup = false }
            )
        }
    }
}

/**
 * 상단 요약/상세 정보 카드 컴포넌트.
 */
@Composable
private fun HistoryInfoCard(
    uiState: HistoryDetailUiState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onImageClick: (path: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .animateContentSize() // 전체 크기 변화 애니메이션
    ) {
        // [1] 헤더 영역 (항상 고정된 위치)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // 중앙: 날짜 + 시간 (고정)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = uiState.dateString,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = uiState.startTimeString,
                    fontSize = 12.sp,
                    color = TextGray
                )
            }

            // 좌측: 뒤로가기 버튼 (세로 중앙 정렬 보정)
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onBackClick)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_left),
                    contentDescription = "뒤로",
                    modifier = Modifier.size(16.dp),
                    tint = PrimaryBlue
                )
                Text(
                    text = "뒤로",
                    color = PrimaryBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.offset(y = (-1).dp)
                )
            }

            // 우측: 삭제 버튼 (뒤로가기 버튼과 동일 선상)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_history_delete),
                    contentDescription = "삭제",
                    modifier = Modifier.size(24.dp),
                    tint = WarningRed
                )
            }
        }

        // [2] 확장 화살표 영역 (헤더의 하단부에 고정, 확장되어도 이동 안함)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(top = 4.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_history_expand),
                contentDescription = if (isExpanded) "접기" else "펼치기",
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        // 현재 상태의 반대로 회전 값 변경
                        rotationZ = if (isExpanded) 0f else 180f
                    },
                tint = Color(0xFFC7C7CC)
            )        }

        // [3] 확장 시 아래로 늘어나는 상세 정보 영역
        if (isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 구분선
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = DividerColor
                )
                
                // 상세 통계 (iOS 스타일 간격 적용)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HistoryStatItem(
                        iconRes = R.drawable.ic_history_clock,
                        label = "낚시 시간",
                        value = uiState.totalDuration,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.size(1.dp, 40.dp).background(DividerColor))
                    HistoryStatItem(
                        iconRes = R.drawable.ic_history_distance,
                        label = "이동 경로",
                        value = "${uiState.stateMarkers.size + uiState.photoMarkers.size}지점",
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.size(1.dp, 40.dp).background(DividerColor))
                    HistoryStatItem(
                        iconRes = R.drawable.ic_history_photo,
                        label = "조과물",
                        value = "${uiState.photoMarkers.size}장",
                        modifier = Modifier.weight(1f)
                    )
                }

                // 조과물 사진 (있을 때만)
                if (uiState.photoMarkers.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = DividerColor
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "조과물 사진",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1C1C1E),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.photoMarkers) { marker ->
                                HistoryPhotoThumbnail(
                                    thumbnailPath = marker.thumbnailPath,
                                    onClick = { onImageClick(marker.thumbnailPath) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 상세 통계 항목 (iOS 스타일 정밀 간격).
 */
@Composable
private fun HistoryStatItem(
    iconRes: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp) // 아이콘-텍스트 간격 4dp
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = TextGray
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextGray
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E)
        )
    }
}

/**
 * 조과물 사진 썸네일.
 */
@Composable
private fun HistoryPhotoThumbnail(
    thumbnailPath: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    AsyncImage(
        model = File(context.filesDir, thumbnailPath),
        contentDescription = "조과물 사진",
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}
