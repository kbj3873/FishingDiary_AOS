package com.onbada.seathermo.presentation.history.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.onbada.seathermo.R
import com.onbada.seathermo.presentation.common.components.CommonPopupOverlay
import com.onbada.seathermo.presentation.common.components.PopupDestructiveColor
import com.onbada.seathermo.presentation.history.viewmodel.HistoryPhotoMarker
import java.io.File
import kotlinx.coroutines.launch

/**
 * 풀스크린 사진 뷰어.
 *
 * [개념] iOS의 HistoryImageViewer.swift (.fullScreenCover)에 대응합니다.
 *        Dialog(usePlatformDefaultWidth = false)로 전체 화면을 덮는 풀스크린 효과를 냅니다.
 *        iOS의 TabView + PageTabViewStyle이 Android의 HorizontalPager에 대응합니다.
 *
 * @param photoMarkers    표시할 사진 마커 목록
 * @param initialIndex    처음 표시할 사진 인덱스 (iOS의 selectedImageIndex)
 * @param onIndexChanged  페이지 스와이프 시 현재 인덱스를 ViewModel에 동기화
 * @param onDeletePhoto   현재 페이지 사진 삭제 요청
 * @param onDismiss       뷰어 닫기
 */
@Composable
fun HistoryImageViewer(
    photoMarkers: List<HistoryPhotoMarker>,
    initialIndex: Int,
    onIndexChanged: (Int) -> Unit,
    onDeletePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    // [개념] Dialog(usePlatformDefaultWidth = false)는 화면 전체를 차지하는 다이얼로그를 만듭니다.
    //        iOS의 .fullScreenCover(isPresented:)에 대응합니다.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        HistoryImageViewerContent(
            photoMarkers   = photoMarkers,
            initialIndex   = initialIndex,
            onIndexChanged = onIndexChanged,
            onDeletePhoto  = onDeletePhoto,
            onDismiss      = onDismiss
        )
    }
}

@Composable
private fun HistoryImageViewerContent(
    photoMarkers: List<HistoryPhotoMarker>,
    initialIndex: Int,
    onIndexChanged: (Int) -> Unit,
    onDeletePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // [개념] rememberCoroutineScope()는 Composable 생명주기에 묶인 코루틴 스코프를 제공합니다.
    //        animateScrollToPage()가 suspend 함수이므로 코루틴 안에서 호출해야 합니다.
    //        iOS의 withAnimation { } 블록에 대응합니다.
    val scope = rememberCoroutineScope()

    // [개념] rememberPagerState는 HorizontalPager의 현재 페이지 상태를 관리합니다.
    //        iOS의 TabView(selection: $viewModel.selectedImageIndex)에 대응합니다.
    val pagerState = rememberPagerState(
        initialPage  = initialIndex.coerceIn(0, (photoMarkers.size - 1).coerceAtLeast(0)),
        pageCount    = { photoMarkers.size }
    )

    var showDeletePopup by remember { mutableStateOf(false) }

    // 페이지 스와이프 시 ViewModel 인덱스 동기화
    // [개념] snapshotFlow는 Compose 상태 변화를 Flow로 변환합니다.
    //        iOS의 onChange(of: viewModel.selectedImageIndex)에 대응합니다.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onIndexChanged(page)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── 1. 이미지 뷰어 (좌우 스와이프) ───────────────────────────────────────
        // iOS의 TabView + PageTabViewStyle에 대응합니다.
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val marker = photoMarkers.getOrNull(page)
            if (marker != null) {
                AsyncImage(
                    model              = File(context.filesDir, marker.thumbnailPath),
                    contentDescription = "조과물 사진",
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Fit
                )
            }
        }

        // ── 2. 상단 컨트롤 (삭제 + 닫기) ─────────────────────────────────────────
        // iOS의 VStack > HStack (trailing) 상단 컨트롤에 대응합니다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, end = 20.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // 삭제 버튼
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { showDeletePopup = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter           = painterResource(id = R.drawable.ic_history_delete),
                    contentDescription = "사진 삭제",
                    modifier          = Modifier.size(24.dp),
                    tint              = Color.White
                )
            }

            // 닫기 버튼 (X)
            // [개념] iOS의 Image(systemName: "xmark")에 대응하는 Material Icons.Default.Close 사용.
            //        SF Symbol과 Material Icons는 모두 시스템 레벨 아이콘 라이브러리이므로
            //        커스텀 PNG 규칙의 예외입니다.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector       = Icons.Default.Close,
                    contentDescription = "닫기",
                    modifier          = Modifier.size(20.dp),
                    tint              = Color.White
                )
            }
        }

        // ── 3. 하단 페이지 인디케이터 (N / M) ────────────────────────────────────
        // iOS의 HStack (이전/페이지텍스트/다음)에 대응합니다.
        if (photoMarkers.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // 이전 버튼
                val hasPrev = pagerState.currentPage > 0
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasPrev) {
                            // [개념] animateScrollToPage는 부드러운 페이지 전환을 수행합니다.
                            //        suspend 함수이므로 scope.launch { } 안에서 호출합니다.
                            //        iOS의 withAnimation { viewModel.selectedImageIndex -= 1 }에 대응합니다.
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter           = painterResource(id = R.drawable.ic_chevron_left),
                        contentDescription = "이전",
                        modifier          = Modifier.size(24.dp),
                        tint              = Color.White.copy(alpha = if (hasPrev) 1f else 0.3f)
                    )
                }

                Spacer(modifier = Modifier.size(40.dp))

                // 페이지 텍스트 (N / M)
                Text(
                    text       = "${pagerState.currentPage + 1} / ${photoMarkers.size}",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color      = Color.White
                )

                Spacer(modifier = Modifier.size(40.dp))

                // 다음 버튼
                val hasNext = pagerState.currentPage < photoMarkers.size - 1
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(enabled = hasNext) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter           = painterResource(id = R.drawable.ic_chevron_right),
                        contentDescription = "다음",
                        modifier          = Modifier.size(24.dp),
                        tint              = Color.White.copy(alpha = if (hasNext) 1f else 0.3f)
                    )
                }
            }
        }

        // ── 4. 사진 삭제 팝업 ─────────────────────────────────────────────────────
        // iOS의 deletePopupOverlay에 대응합니다.
        if (showDeletePopup) {
            CommonPopupOverlay(
                title               = "사진 삭제",
                message             = "이 사진을 삭제하시겠습니까?\n삭제된 데이터는 복구할 수 없습니다.",
                primaryButtonText   = "삭제",
                primaryButtonColor  = PopupDestructiveColor,
                onPrimaryClick      = {
                    showDeletePopup = false
                    onDeletePhoto()
                    // 남은 사진 없으면 ViewModel의 deletePhoto가 isImageViewerPresented = false 처리
                },
                secondaryButtonText = "취소",
                onSecondaryClick    = { showDeletePopup = false },
                onDismiss           = { showDeletePopup = false }
            )
        }
    }
}
