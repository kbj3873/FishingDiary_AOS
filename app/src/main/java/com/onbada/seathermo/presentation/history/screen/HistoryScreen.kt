package com.onbada.seathermo.presentation.history.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.painterResource
import com.onbada.seathermo.R
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.onbada.seathermo.application.di.ApplicationDIContainer
import com.onbada.seathermo.presentation.history.viewmodel.HistoryRecordItem
import com.onbada.seathermo.presentation.history.viewmodel.HistoryViewModel

// ─── 공통 텍스트 스타일 ────────────────────────────────────────────────────────
private val NoFontPaddingStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

// ─── 색상 상수 (Figma 기준) ───────────────────────────────────────────────────
private val BackgroundColor      = Color(0xFFF2F2F7)
private val HeaderBackground     = Color.White
private val CardBackground       = Color.White
private val TitleColor           = Color(0xFF1F2937)
private val SubtitleColor        = Color(0xFF8E8E93)
private val DateColor            = Color.Black
private val SeparatorDotColor    = Color(0xFFC7C7CC)
private val MetaTextColor        = Color(0xFF8E8E93)
private val MetaIconColor        = Color(0xFF9CA3AF)
private val ThumbnailPlaceholder = Color(0xFFF2F2F7)
private val ChevronColor         = Color(0xFFC7C7CC)
private val HeaderDividerColor   = Color(0xFFE5E5EA)

/**
 * 히스토리 탭 내부 Navigation Host.
 *
 * @param isVisible 현재 히스토리 탭이 활성화되어 있는지 여부. (MainTabScreen에서 전달)
 */
@Composable
fun HistoryTabNavHost(
    diContainer: ApplicationDIContainer,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "history_list",
        modifier = modifier.fillMaxSize()
    ) {
        // 히스토리 목록 화면
        composable("history_list") {
            val historyViewModel: HistoryViewModel = viewModel(
                factory = diContainer.makeHistoryViewModelFactory()
            )

            HistoryScreen(
                viewModel = historyViewModel,
                isVisible = isVisible, // isVisible 상태 전달
                onNavigateToDetail = { sessionId ->
                    navController.navigate("history_detail/$sessionId")
                }
            )
        }
        // 히스토리 상세 화면
        // HistoryImageViewer는 HistoryDetailScreen 내부에서 Dialog로 처리되므로
        // 별도 Navigation Route가 필요 없습니다.
        composable("history_detail/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            HistoryDetailScreen(
                sessionId       = sessionId,
                diContainer     = diContainer,
                onNavigateBack  = { shouldRefresh ->
                    if (shouldRefresh) {
                        // 목록 화면으로 돌아갈 때 데이터가 변경된 경우를 대비해
                        // HistoryScreen의 LaunchedEffect(isVisible)이 자동으로 재로딩합니다.
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * 낚시 히스토리 목록 화면.
 *
 * @param isVisible 현재 탭 활성화 여부. true가 될 때마다 데이터를 갱신합니다.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    isVisible: Boolean,
    onNavigateToDetail: (sessionId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // [변경] 탭이 "활성화"될 때마다 목록을 강제로 갱신합니다.
    // Persistent 렌더링 방식에서는 ON_RESUME이 발생하지 않으므로 이 방식이 가장 확실합니다.
    LaunchedEffect(isVisible) {
        if (isVisible) {
            viewModel.loadRecords()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // 헤더 영역
        HistoryHeader()

        // 컨텐츠 영역
        when {
            uiState.isLoading && uiState.records.isEmpty() -> HistoryLoadingView()
            uiState.records.isEmpty() -> HistoryEmptyView()
            else -> HistoryRecordList(
                records = uiState.records,
                onNavigateToDetail = onNavigateToDetail
            )
        }
    }
}

// ─── 헤더 ─────────────────────────────────────────────────────────────────────

@Composable
private fun HistoryHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "낚시 히스토리",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TitleColor,
                letterSpacing = (-0.35).sp
            )
            Text(
                text = "과거 낚시 기록을 확인하세요",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = SubtitleColor,
                letterSpacing = (-0.15).sp
            )
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = HeaderDividerColor
        )
    }
}

// ─── 기록 목록 ────────────────────────────────────────────────────────────────

@Composable
private fun HistoryRecordList(
    records: List<HistoryRecordItem>,
    onNavigateToDetail: (sessionId: String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = records,
            key = { it.id }
        ) { record ->
            HistoryRecordCardView(
                item = record,
                onClick = { onNavigateToDetail(record.id) }
            )
        }
    }
}

// ─── 카드 컴포넌트 ─────────────────────────────────────────────────────────────

@Composable
private fun HistoryRecordCardView(
    item: HistoryRecordItem,
    onClick: () -> Unit
) {
    Row(
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
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HistoryThumbnailView(thumbnailPath = item.thumbnailPath)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = item.formattedDate,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DateColor,
                    letterSpacing = (-0.31).sp
                )
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = "•",
                    fontSize = 16.sp,
                    color = SeparatorDotColor
                )
                Text(
                    modifier = Modifier.alignByBaseline().graphicsLayer { translationY = -2f },
                    text = item.startTime,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MetaTextColor,
                    letterSpacing = (-0.15).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HistoryMetaItem(
                    iconResId = R.drawable.ic_history_distance,
                    label = "${item.pointCount}지점"
                )
                HistoryMetaItem(
                    iconResId = R.drawable.ic_history_photo,
                    label = "${item.photoCount}장"
                )
                HistoryMetaItem(
                    iconResId = R.drawable.ic_history_clock,
                    label = item.duration,
                    maxLines = 1
                )
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = ChevronColor
        )
    }
}

@Composable
private fun HistoryThumbnailView(thumbnailPath: String?) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ThumbnailPlaceholder),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailPath != null) {
            AsyncImage(
                model = File(context.filesDir, thumbnailPath),
                contentDescription = "낚시 기록 썸네일",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_history_fish),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MetaIconColor
            )
        }
    }
}

@Composable
private fun HistoryMetaItem(
    iconResId: Int,
    label: String,
    maxLines: Int = Int.MAX_VALUE
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(14.dp).offset(y = 1.dp),
            tint = MetaIconColor
        )
        Text(
            text = label,
            style = NoFontPaddingStyle.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MetaTextColor,
                letterSpacing = (-0.15).sp
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistoryEmptyView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_history_fish),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color(0xFF9CA3AF)
            )
            Text(
                text = "기록된 낚시가 없습니다",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7280)
            )
            Text(
                text = "낚시기록 탭에서 새로운 낚시를 시작해보세요",
                fontSize = 14.sp,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
private fun HistoryLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = Color(0xFF2563EB)
            )
            Text(
                text = "기록을 불러오는 중...",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}
