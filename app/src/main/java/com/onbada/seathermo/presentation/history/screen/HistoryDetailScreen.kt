package com.onbada.seathermo.presentation.history.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.onbada.seathermo.R
import com.onbada.seathermo.application.di.ApplicationDIContainer
import com.onbada.seathermo.presentation.common.components.CommonPopupOverlay
import com.onbada.seathermo.presentation.common.components.PopupDestructiveColor
import com.onbada.seathermo.presentation.history.viewmodel.HistoryDetailUiState
import com.onbada.seathermo.presentation.history.viewmodel.HistoryDetailViewModel
import com.onbada.seathermo.presentation.history.viewmodel.SelectedMarkerInfo
import java.io.File

// ─── 색상 상수 (iOS/Figma 기준) ────────────────────────────────────────────────
private val PrimaryBlue = Color(0xFF2563EB)
private val TextBlack = Color(0xFF1C1C1E)
private val TextGray = Color(0xFF8E8E93)
private val WarningRed = Color(0xFFFF3B30)
private val DividerColor = Color(0xFFE5E5EA)

/**
 * 히스토리 상세 화면.
 *
 * [구조] iOS의 HistoryDetailView.swift에 대응합니다.
 *        ZStack(Box) 기반: 지도(배경) → 상단 카드 → 하단 마커 오버레이 카드 → 삭제 팝업 순서로 겹칩니다.
 */
@Composable
fun HistoryDetailScreen(
    sessionId: String,
    diContainer: ApplicationDIContainer,
    onNavigateBack: (shouldRefresh: Boolean) -> Unit
) {
    val viewModel: HistoryDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = diContainer.makeHistoryDetailViewModelFactory(sessionId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showDeletePopup by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    // 진입 시 데이터 로드
    LaunchedEffect(Unit) {
        viewModel.fetchSessionData()
    }

    // 세션 삭제 완료 감지 → 뒤로가기 (목록 갱신 필요)
    LaunchedEffect(uiState.shouldDismiss) {
        if (uiState.shouldDismiss) {
            onNavigateBack(true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ── 1. 지도 배경 (전체 화면) ────────────────────────────────────────────
        // iOS의 HistoryMapView / HistoryKakaoMapView에 대응합니다.
        GoogleHistoryMapView(
            polylines     = uiState.polylines,
            stateMarkers  = uiState.stateMarkers,
            photoMarkers  = uiState.photoMarkers,
            onMarkerClick = { markerId -> viewModel.selectMarker(markerId) },
            modifier      = Modifier.fillMaxSize()
        )

        // ── 2. 상단 정보 카드 ────────────────────────────────────────────────────
        HistoryInfoCard(
            uiState        = uiState,
            isExpanded     = isExpanded,
            onToggleExpand = { isExpanded = !isExpanded },
            onBackClick    = { onNavigateBack(uiState.isDataModified) },
            onDeleteClick  = { showDeletePopup = true },
            onImageClick   = { index -> viewModel.openImageViewer(index) },
            modifier       = Modifier
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter)
        )

        // ── 3. 하단 마커 선택 오버레이 카드 ─────────────────────────────────────
        // iOS의 selectedMarker 오버레이: 상태 마커 vs 사진 마커 분기
        val selectedMarker = uiState.selectedMarker
        if (selectedMarker != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 34.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (selectedMarker.thumbnailPath == null) {
                    StateMarkerCard(
                        marker    = selectedMarker,
                        onDismiss = { viewModel.clearSelectedMarker() }
                    )
                } else {
                    PhotoMarkerCard(
                        marker       = selectedMarker,
                        onDismiss    = { viewModel.clearSelectedMarker() },
                        onImageClick = {
                            val index = uiState.photoMarkers.indexOfFirst {
                                it.thumbnailPath == selectedMarker.thumbnailPath
                            }.coerceAtLeast(0)
                            viewModel.openImageViewer(index)
                        }
                    )
                }
            }
        }

        // ── 4. 삭제 확인 팝업 ────────────────────────────────────────────────────
        if (showDeletePopup) {
            CommonPopupOverlay(
                title               = "낚시 기록 삭제",
                message             = "이 낚시 기록을 삭제하시겠습니까? 삭제된 기록은 복구할 수 없습니다.",
                primaryButtonText   = "삭제",
                primaryButtonColor  = PopupDestructiveColor,
                onPrimaryClick      = {
                    showDeletePopup = false
                    viewModel.deleteRecord()
                },
                secondaryButtonText = "취소",
                onSecondaryClick    = { showDeletePopup = false },
                onDismiss           = { showDeletePopup = false }
            )
        }
    }

    // ── 5. 이미지 뷰어 (풀스크린 Dialog) ─────────────────────────────────────────
    // iOS의 .fullScreenCover(isPresented: $viewModel.isImageViewerPresented)에 대응합니다.
    if (uiState.isImageViewerPresented) {
        HistoryImageViewer(
            photoMarkers       = uiState.photoMarkers,
            initialIndex       = uiState.selectedImageIndex,
            onIndexChanged     = { viewModel.setSelectedImageIndex(it) },
            onDeletePhoto      = { viewModel.deletePhoto(uiState.selectedImageIndex) },
            onDismiss          = { viewModel.closeImageViewer() }
        )
    }
}

// ─── 상단 정보 카드 ────────────────────────────────────────────────────────────

/**
 * 상단 요약/상세 정보 카드.
 * iOS의 headerCardView에 대응합니다.
 */
@Composable
private fun HistoryInfoCard(
    uiState: HistoryDetailUiState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onBackClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onImageClick: (initialIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .animateContentSize()
    ) {
        // [1] 헤더 (뒤로가기 / 날짜+시간 / 삭제)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // 중앙: 날짜 + 출발 시간
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text       = uiState.dateString,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.Black
                )
                Text(
                    text     = uiState.startTimeString,
                    fontSize = 12.sp,
                    color    = TextGray
                )
            }

            // 좌측: 뒤로가기
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onBackClick)
                    .padding(vertical = 4.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter           = painterResource(id = R.drawable.ic_chevron_left),
                    contentDescription = "뒤로",
                    modifier          = Modifier.size(16.dp),
                    tint              = PrimaryBlue
                )
                Text(
                    text       = "뒤로",
                    color      = PrimaryBlue,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier   = Modifier.offset(y = (-1).dp)
                )
            }

            // 우측: 삭제
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDeleteClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter           = painterResource(id = R.drawable.ic_history_delete),
                    contentDescription = "삭제",
                    modifier          = Modifier.size(24.dp),
                    tint              = WarningRed
                )
            }
        }

        // [2] 확장/축소 화살표
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(top = 4.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter           = painterResource(id = R.drawable.ic_history_expand),
                contentDescription = if (isExpanded) "접기" else "펼치기",
                modifier          = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = if (isExpanded) 0f else 180f },
                tint = Color(0xFFC7C7CC)
            )
        }

        // [3] 확장 영역 (상세 통계 + 사진 목록)
        if (isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(
                    modifier  = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color     = DividerColor
                )

                // 통계 3열
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HistoryStatItem(
                        iconRes  = R.drawable.ic_history_clock,
                        label    = "낚시 시간",
                        value    = uiState.totalDuration,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.size(1.dp, 40.dp).background(DividerColor))
                    HistoryStatItem(
                        iconRes  = R.drawable.ic_history_distance,
                        label    = "이동 경로",
                        value    = "${uiState.stateMarkers.size + uiState.photoMarkers.size}지점",
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.size(1.dp, 40.dp).background(DividerColor))
                    HistoryStatItem(
                        iconRes  = R.drawable.ic_history_photo,
                        label    = "조과물",
                        value    = "${uiState.photoMarkers.size}장",
                        modifier = Modifier.weight(1f)
                    )
                }

                // 조과물 사진 (있을 때만)
                if (uiState.photoMarkers.isNotEmpty()) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color     = DividerColor
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text       = "조과물 사진",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF1C1C1E),
                            modifier   = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding        = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.photoMarkers) { marker ->
                                val index = uiState.photoMarkers.indexOf(marker)
                                HistoryPhotoThumbnail(
                                    thumbnailPath = marker.thumbnailPath,
                                    onClick       = { onImageClick(index) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── 하단 마커 오버레이 카드 ──────────────────────────────────────────────────

/**
 * 상태 마커 탭 시 표시되는 하단 카드.
 * iOS의 stateMarkerCard에 대응합니다.
 */
@Composable
private fun StateMarkerCard(
    marker: SelectedMarkerInfo,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.95f))
            .padding(horizontal = 16.dp)
            .height(56.dp),
        verticalAlignment    = Alignment.CenterVertically
    ) {
        // 지점명 (아이콘 + 텍스트)
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter           = painterResource(id = R.drawable.ic_history_distance),
                contentDescription = null,
                modifier          = Modifier.size(16.dp),
                tint              = TextGray
            )
            Text(
                text       = marker.title,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.Black,
                modifier   = Modifier.graphicsLayer { translationY = -3f }
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 시간 (아이콘 + 텍스트)
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter           = painterResource(id = R.drawable.ic_history_clock),
                contentDescription = null,
                modifier          = Modifier.size(16.dp),
                tint              = TextGray
            )
            Text(
                text     = marker.timeString,
                fontSize = 15.sp,
                color    = TextGray
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 닫기 버튼 — iOS의 Image(systemName: "xmark") size 14 medium gray에 대응
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector       = Icons.Default.Close,
                contentDescription = "닫기",
                modifier          = Modifier.size(14.dp),
                tint              = TextGray
            )
        }
    }
}

/**
 * 사진 마커 탭 시 표시되는 하단 카드.
 * iOS의 photoMarkerCard에 대응합니다.
 */
@Composable
private fun PhotoMarkerCard(
    marker: SelectedMarkerInfo,
    onDismiss: () -> Unit,
    onImageClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 썸네일 (탭 → 이미지 뷰어)
        AsyncImage(
            model              = File(context.filesDir, marker.thumbnailPath!!),
            contentDescription = "조과물 사진",
            modifier           = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onImageClick),
            contentScale = ContentScale.Crop
        )

        // 우측 정보 영역
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 지점명 + 닫기
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = marker.title,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.Black,
                    modifier   = Modifier.weight(1f)
                )
                // 닫기 버튼 — iOS의 Image(systemName: "xmark") size 14 gray에 대응
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector       = Icons.Default.Close,
                        contentDescription = "닫기",
                        modifier          = Modifier.size(14.dp),
                        tint              = TextGray
                    )
                }
            }

            // 시간
            Row(
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter           = painterResource(id = R.drawable.ic_history_clock),
                    contentDescription = null,
                    modifier          = Modifier.size(12.dp),
                    tint              = TextGray
                )
                Text(
                    text     = marker.timeString,
                    fontSize = 14.sp,
                    color    = TextGray
                )
            }

            // "이 지점의 조과물" 버튼
            Text(
                text       = "이 지점의 조과물",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = PrimaryBlue,
                modifier   = Modifier
                    .clickable(onClick = onImageClick)
                    .padding(top = 2.dp)
            )
        }
    }
}

// ─── 공통 서브컴포넌트 ─────────────────────────────────────────────────────────

@Composable
private fun HistoryStatItem(
    iconRes: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier              = modifier,
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter           = painterResource(id = iconRes),
            contentDescription = null,
            modifier          = Modifier.size(16.dp),
            tint              = TextGray
        )
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextGray)
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextBlack)
    }
}

@Composable
private fun HistoryPhotoThumbnail(
    thumbnailPath: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    AsyncImage(
        model              = File(context.filesDir, thumbnailPath),
        contentDescription = "조과물 사진",
        modifier           = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}
