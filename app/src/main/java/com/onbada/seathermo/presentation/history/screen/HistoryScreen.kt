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
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
// [개념] Android Compose Text는 기본적으로 includeFontPadding = true 상태입니다.
//        이 보이지 않는 여백(ascender/descender padding) 때문에
//        CenterVertically 정렬이 시각적으로 어긋나 보입니다.
//        includeFontPadding = false + LineHeightStyle.Trim.Both 를 적용하면
//        패딩이 제거되어 CenterVertically가 시각적으로 정확하게 동작합니다.
//        Text+Text, Icon+Text 모든 조합에서 사용합니다.
// [개념] includeFontPadding = false만 제거 — Trim 없이 사용.
//        Icon + Text 조합에서 CenterVertically가 시각적으로 정확히 동작합니다.
private val NoFontPaddingStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

// [개념] 날짜+시간처럼 폰트 크기가 다른 Text를 같은 Row에 배치할 때,
//        두 Text에 동일한 lineHeight를 주면 Text 박스 높이가 같아져서
//        CenterVertically가 글자 중앙을 정확히 맞춥니다.
//        Figma 기준: 날짜 lineHeight=24, 시간 lineHeight=20 이지만
//        두 항목이 같은 Row이므로 더 큰 값(24)으로 통일합니다.
private val DateRowTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeight = 24.sp
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
 * [개념] 각 탭이 독립적인 NavController를 가지는 "중첩 내비게이션(Nested Navigation)" 패턴입니다.
 *        iOS의 NavigationStack(path: $viewModel.path)와 동일한 역할을 합니다.
 *        탭 내부에서 뒤로가기를 누르면 다른 탭으로 이동하지 않고 탭 내 이전 화면으로 돌아갑니다.
 *
 * @param diContainer 의존성 주입 컨테이너. ViewModel 생성에 사용됩니다.
 */
@Composable
fun HistoryTabNavHost(
    diContainer: ApplicationDIContainer,
    modifier: Modifier = Modifier
) {
    // [개념] rememberNavController()는 이 Composable 스코프에 묶인 NavController를 생성합니다.
    //        MainTabScreen의 NavController와 별개의 백스택을 관리하므로
    //        히스토리 내부 화면(목록 → 상세)을 독립적으로 처리합니다.
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "history_list",
        modifier = modifier.fillMaxSize()
    ) {
        // 히스토리 목록 화면
        composable("history_list") {
            // [개념] viewModel()은 NavBackStackEntry 스코프에 묶인 ViewModel을 생성합니다.
            //        이 route가 백스택에 있는 한 동일 인스턴스가 재사용됩니다.
            val historyViewModel: HistoryViewModel = viewModel(
                factory = diContainer.makeHistoryViewModelFactory()
            )
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateToDetail = { sessionId ->
                    navController.navigate("history_detail/$sessionId")
                }
            )
        }
        // 히스토리 상세 화면 (TODO: 다음 단계에서 구현)
        composable("history_detail/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            // TODO: HistoryDetailScreen(sessionId = sessionId, diContainer = diContainer, onNavigateBack = { navController.popBackStack() })
        }
    }
}

/**
 * 낚시 히스토리 목록 화면.
 *
 * [개념] @Composable 함수는 상태(State)가 변경될 때마다 Compose 런타임에 의해 재호출(Recomposition)됩니다.
 *        iOS의 SwiftUI View body와 동일한 원리입니다.
 *
 * @param viewModel 히스토리 데이터를 관리하는 ViewModel.
 * @param onNavigateToDetail 카드 탭 시 호출되는 상세 화면 이동 콜백. sessionId를 전달합니다.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToDetail: (sessionId: String) -> Unit = {}
) {
    // [개념] collectAsStateWithLifecycle()은 Flow를 Compose State로 변환하되,
    //        화면이 백그라운드일 때 자동으로 수집을 중단합니다(배터리 절약).
    //        Swift의 @StateObject + @Published 조합과 동일한 역할입니다.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // [개념] LaunchedEffect(Unit)은 이 Composable이 화면에 처음 나타날 때 한 번 코루틴을 실행합니다.
    //        Unit은 "변하지 않는 키"이므로 재실행되지 않습니다.
    //        iOS의 .onAppear { }와 동일하지만, ViewModel에 데이터가 없을 때만 로드하는 로직은
    //        ViewModel 측에서 관리합니다.
    LaunchedEffect(Unit) {
        if (uiState.records.isEmpty() && !uiState.isLoading) {
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
            uiState.isLoading -> HistoryLoadingView()
            uiState.records.isEmpty() -> HistoryEmptyView()
            else -> HistoryRecordList(
                records = uiState.records,
                onNavigateToDetail = onNavigateToDetail
            )
        }
    }
}

// ─── 헤더 ─────────────────────────────────────────────────────────────────────

/**
 * 화면 상단 헤더. 타이틀과 서브타이틀을 표시합니다.
 *
 * Figma: 흰색 배경, paddingTop 48dp, paddingHorizontal 16dp, 하단 구분선 #E5E5EA
 */
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
            // Figma: 30sp, Bold, Black, letterSpacing -0.35px
            Text(
                text = "낚시 히스토리",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TitleColor,
                letterSpacing = (-0.35).sp
            )
            // Figma: 14sp, Regular, #8E8E93, letterSpacing -0.15px
            Text(
                text = "과거 낚시 기록을 확인하세요",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = SubtitleColor,
                letterSpacing = (-0.15).sp
            )
        }
        // Figma: 하단 구분선 #E5E5EA, 0.5dp
        HorizontalDivider(
            thickness = 0.5.dp,
            color = HeaderDividerColor
        )
    }
}

// ─── 기록 목록 ────────────────────────────────────────────────────────────────

/**
 * 낚시 기록 카드 목록.
 *
 * [개념] LazyColumn은 SwiftUI의 LazyVStack 또는 UIKit의 UITableView와 동일합니다.
 *        화면에 보이는 항목만 렌더링하므로 대량 데이터에서도 성능이 유지됩니다.
 */
@Composable
private fun HistoryRecordList(
    records: List<HistoryRecordItem>,
    onNavigateToDetail: (sessionId: String) -> Unit
) {
    LazyColumn(
        // Figma: paddingHorizontal 16dp, paddingTop 24dp, paddingBottom 24dp
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = records,
            // [개념] key = { }를 지정하면 리스트 변경 시 Compose가 각 항목을 id로 추적합니다.
            //        불필요한 Recomposition을 방지하고 애니메이션이 올바르게 동작합니다.
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

/**
 * 히스토리 기록 카드 컴포넌트.
 *
 * Figma: 흰색 배경, cornerRadius 16dp, shadow(0,0,0,0.04)
 * 내부 padding 16dp, 썸네일(64x64) + 정보 영역 + 화살표
 *
 * @param item 표시할 낚시 기록 항목.
 * @param onClick 카드 탭 시 호출되는 콜백.
 */
@Composable
private fun HistoryRecordCardView(
    item: HistoryRecordItem,
    onClick: () -> Unit
) {
    // [개념] shadow()는 Material elevation 대신 커스텀 그림자를 만들 때 사용합니다.
    //        clip()으로 shadow가 카드 모서리 밖으로 넘치지 않도록 처리합니다.
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
        // 썸네일 이미지 (64x64)
        HistoryThumbnailView(thumbnailPath = item.thumbnailPath)

        // 정보 영역 (날짜 + 메타데이터)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 날짜 + 출발 시간 행
            // [개념] 폰트 크기가 다른 Text끼리 (16sp vs 14sp) 정렬할 때는
            //        alignByBaseline()이 가장 자연스럽습니다.
            //        각 Text의 글자 바닥선(baseline)을 일치시켜 타이포그래피적으로 올바른 정렬을 만듭니다.
            //        includeFontPadding = false + CenterVertically 조합은
            //        오히려 작은 크기 텍스트가 아래로 떨어지는 문제가 있으므로 사용하지 않습니다.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Figma: 16sp, SemiBold, Black
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = item.formattedDate,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DateColor,
                    letterSpacing = (-0.31).sp
                )
                // Figma: 구분점 •, #C7C7CC
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = "•",
                    fontSize = 16.sp,
                    color = SeparatorDotColor
                )
                // Figma: 14sp, Regular, #8E8E93
                // [개념] graphicsLayer { translationY }는 드로우 단계에서만 이동하므로
                //        Row 측정에 전혀 영향을 주지 않습니다. translationY 단위는 픽셀입니다.
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

            // 메타데이터 행 (지점 수 / 사진 수 / 소요 시간)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 지점 수 (Figma: distance_icon, iOS distance_icon.imageset)
                HistoryMetaItem(
                    iconResId = R.drawable.ic_history_distance,
                    label = "${item.pointCount}지점"
                )
                // 사진 수 (Figma: photo_icon, iOS photo_icon.imageset)
                HistoryMetaItem(
                    iconResId = R.drawable.ic_history_photo,
                    label = "${item.photoCount}장"
                )
                // 소요 시간 (Figma: clock_icon, iOS clock_icon.imageset)
                HistoryMetaItem(
                    iconResId = R.drawable.ic_history_clock,
                    label = item.duration,
                    maxLines = 1
                )
            }
        }

        // 오른쪽 화살표 아이콘 (Figma: 20dp, ic_chevron_right.png)
        // [개념] painterResource()는 res/drawable-*/에 저장된 PNG를 밀도에 맞게 불러옵니다.
        //        iOS의 Image("ic_chevron_right")와 동일합니다.
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = ChevronColor
        )
    }
}

/**
 * 카드 썸네일 이미지 영역.
 *
 * 사진이 있으면 AsyncImage로 로컬 파일을 로드하고,
 * 없으면 회색 배경 + 카메라 아이콘 플레이스홀더를 표시합니다.
 *
 * [개념] AsyncImage는 Coil 라이브러리의 Compose 컴포넌트입니다.
 *        model에 파일 경로(String)를 넣으면 비동기로 이미지를 로드합니다.
 *        iOS의 AsyncImage(url:) 또는 Image(uiImage:) + 파일 로드 로직과 동일합니다.
 *
 * @param thumbnailPath 로컬 파일 경로. null이면 플레이스홀더를 표시합니다.
 */
@Composable
private fun HistoryThumbnailView(thumbnailPath: String?) {
    // Figma: 64x64dp, cornerRadius 12dp
    // [개념] LocalContext.current는 현재 Composable 스코프의 Android Context를 가져옵니다.
    //        iOS의 FileManager.default와 동일하게 파일 경로 조합에 사용합니다.
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ThumbnailPlaceholder),
        contentAlignment = Alignment.Center
    ) {
        if (thumbnailPath != null) {
            // FishingRecordViewModel은 이미지를 context.filesDir 기준 상대 경로로 저장합니다.
            // Coil은 File 객체를 받으면 filesDir 내 파일을 올바르게 로드합니다.
            // [개념] File(dir, relativePath)는 filesDir + "/" + thumbnailPath 의 절대 경로를 만듭니다.
            //        iOS의 documentsDirectory.appendingPathComponent(path)와 동일합니다.
            AsyncImage(
                model = File(context.filesDir, thumbnailPath),
                contentDescription = "낚시 기록 썸네일",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 플레이스홀더 — 사진 없음 (Figma: fish/lure 아이콘, ic_history_fish.png)
            Icon(
                painter = painterResource(id = R.drawable.ic_history_fish),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MetaIconColor
            )
        }
    }
}

/**
 * 아이콘 + 텍스트로 구성된 메타데이터 항목 (지점 수, 사진 수, 소요 시간).
 *
 * Figma: 아이콘 14dp + gap 4dp + 텍스트 14sp #8E8E93
 * iOS 에셋 PNG → Android drawable-xxhdpi 등 밀도별 폴더의 ic_history_*.png
 *
 * @param iconResId drawable 리소스 ID (ic_history_distance / photo / clock).
 */
@Composable
private fun HistoryMetaItem(
    iconResId: Int,
    label: String,
    maxLines: Int = Int.MAX_VALUE
) {
    // [개념] Icon + Text 조합에서 alignByBaseline()은 사용 불가 (Icon에 text baseline 없음).
    //        CenterVertically + Text에 NoFontPaddingStyle 적용으로
    //        아이콘과 텍스트의 시각적 중앙을 일치시킵니다.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // [개념] painterResource()는 밀도(dpi)에 맞는 drawable-*/ 폴더의 PNG를 자동으로 선택합니다.
        //        Icon composable의 tint 파라미터가 ColorFilter(SRC_IN)로 PNG 색상을 덮어씁니다.
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

// ─── 빈 상태 / 로딩 ───────────────────────────────────────────────────────────

/**
 * 기록이 없을 때 표시되는 빈 상태 화면.
 *
 * iOS의 emptyView와 동일합니다.
 */
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
            // [개념] Icon의 modifier.size()로 크기를 지정합니다.
            //        iOS의 .font(.system(size: 48))와 동일합니다.
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

/**
 * 데이터 로딩 중 표시되는 로딩 화면.
 */
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
            // [개념] CircularProgressIndicator는 iOS의 ProgressView()와 동일한 로딩 스피너입니다.
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
