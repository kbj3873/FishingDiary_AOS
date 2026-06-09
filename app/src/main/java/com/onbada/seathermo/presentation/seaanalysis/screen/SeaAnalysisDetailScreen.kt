package com.onbada.seathermo.presentation.seaanalysis.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.onbada.seathermo.R
import com.onbada.seathermo.application.di.ApplicationDIContainer
import com.onbada.seathermo.common.utils.AppPreferences
import com.onbada.seathermo.common.utils.PreferenceConstants
import com.onbada.seathermo.domain.entity.Region
import com.onbada.seathermo.presentation.seaanalysis.viewmodel.SeaAnalysisDetailUiState
import com.onbada.seathermo.presentation.seaanalysis.viewmodel.SeaAnalysisDetailViewModel
import kotlin.math.ceil
import kotlin.math.floor

// ── 색상 상수 ───────────────────────────────────────────────────────────────
// Figma 노드 406:2157 ~ 406:2200 기준 정확한 색상
private val PageBackground  = Color(0xFFF2F2F7)
private val GraphBackground = Color(0xFF2C303E)         // 그래프 카드 다크 배경
private val NavBarBlue      = Color(0xFF2563EB)          // 뒤로가기 버튼 (blue-600)
private val SubtitleGray    = Color(0xFF8E8E93)

// Figma 수온 카드 색상 (iOS 시스템 색상과 다름 — Figma 기준 적용)
private val SurfaceColor = Color(0xFF3B82F6)  // Figma blue-500
private val MiddleColor  = Color(0xFF8B5CF6)  // Figma violet-500
private val BottomColor  = Color(0xFF10B981)  // Figma emerald-500

// Figma Footer 색상
private val FooterBgColor   = Color(0xFF3B82F6).copy(alpha = 0.1f)  // rgba(59,130,246,0.1)
private val FooterTextColor = Color(0xFF1E40AF)                       // blue-800

/**
 * 수온 분석 상세 화면 — 커스텀 NavBar + 그래프 + 수온 카드.
 *
 * [개념] 이 화면은 NavHost에서 route 파라미터로 stationCode를 받아
 *        AppPreferences에서 Region을 조회한 뒤 ViewModel에 주입합니다.
 *        iOS의 SeaAnalysisDetailView.swift에 대응합니다.
 *
 * @param stationCode 관측소 코드 (regionCode). NavArgs로 전달됩니다.
 * @param diContainer ViewModel factory를 제공하는 DI 컨테이너
 * @param onNavigateBack 뒤로가기 버튼 탭 시 호출 (navController.popBackStack 용도)
 */
@Composable
fun SeaAnalysisDetailScreen(
    stationCode: String,
    diContainer: ApplicationDIContainer,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // stationCode로 전체 지역 목록에서 Region 조회.
    // [개념] remember(stationCode)로 stationCode가 바뀔 때만 재계산합니다.
    val region = remember(stationCode) {
        AppPreferences.getFromList<Region>(context, PreferenceConstants.ALL_REGION_LIST)
            .firstOrNull { it.regionCode == stationCode }
    }

    // region을 찾지 못한 경우 즉시 뒤로 이동
    if (region == null) {
        LaunchedEffect(Unit) { onNavigateBack() }
        return
    }

    val viewModel: SeaAnalysisDetailViewModel = viewModel(
        key = stationCode,
        factory = diContainer.makeSeaAnalysisDetailViewModelFactory(region)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // [개념] LaunchedEffect(Unit)은 화면 최초 진입 시 한 번만 실행됩니다.
    //        iOS의 .onAppear { viewModel.fetchTemperatureData() }에 대응합니다.
    LaunchedEffect(Unit) {
        viewModel.fetchTemperatureData()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 커스텀 네비게이션 바 ────────────────────────────────────────────
            // statusBarsPadding()으로 iOS safe area 상단 여백과 동일하게 처리합니다.
            DetailNavBar(onNavigateBack = onNavigateBack)

            // ── 스크롤 콘텐츠 ──────────────────────────────────────────────────
            // Figma 섹션 간격 = 16dp (iOS VStack(spacing:20)과 다름 — Figma 기준 우선)
            // [개념] navigationBarsPadding()은 Android 제스처 NavBar(하단 시스템 영역)
            //        높이만큼 bottom padding을 추가합니다. SeaAnalysisDetailScreen은
            //        MainTabScreen의 Scaffold 밖(전체화면 NavHost)에 있어 탭바가 없으므로
            //        직접 처리해야 합니다. iOS의 .ignoresSafeArea() 없이 safe area 내부
            //        배치하는 것과 동일합니다.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ── 헤더: 지역명 + 부제목 ────────────────────────────────────────
                HeaderSection(stationName = uiState.stationName)

                // ── 그래프 섹션 (다크 카드) ───────────────────────────────────────
                GraphSection(uiState = uiState)

                // ── 수온 카드 섹션 (표층/중층/저층) ──────────────────────────────
                TemperatureCardsSection(uiState = uiState)

                // ── 하단 안내 문구 ────────────────────────────────────────────────
                FooterSection(uiState = uiState)

                // Figma 하단 여백: content 끝 ~ 화면 하단 = 24dp
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── 커스텀 네비게이션 바 ──────────────────────────────────────────────────────

/**
 * 뒤로가기 버튼만 있는 커스텀 네비게이션 바.
 *
 * [개념] statusBarsPadding()은 Compose WindowInsets API로 시스템 상태바 높이만큼
 *        패딩을 자동 적용합니다. iOS의 .ignoresSafeArea() 없이 safe area 내부에
 *        콘텐츠를 배치하는 것과 동일한 효과입니다.
 *
 * iOS: HStack { Button { HStack { chevron.left + "뒤로" } }, Spacer }
 *      .padding(.horizontal, 16).padding(.vertical, 12).background(#F2F2F7)
 */
@Composable
private fun DetailNavBar(onNavigateBack: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PageBackground)
            // [개념] statusBarsPadding()은 상태바 높이를 top padding으로 추가합니다.
            //        Android 12+에서도 동작하며 iOS safe area top과 동일한 역할입니다.
            .padding(start = 16.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onNavigateBack
                )
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_left),
                contentDescription = null,
                tint = NavBarBlue,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = "뒤로",
                fontSize = 17.sp,
                color = NavBarBlue
            )
        }
    }
}

// ── 헤더 섹션 ──────────────────────────────────────────────────────────────

/**
 * 지역명(34sp Bold) + "수온 변화 추이" 부제목.
 *
 * iOS: VStack(alignment: .leading, spacing: 8)
 *      .padding(.horizontal, 20).padding(.top, 10)
 */
@Composable
private fun HeaderSection(stationName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stationName,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Text(
            text = "수온 변화 추이",
            fontSize = 17.sp,
            color = SubtitleGray
        )
    }
}

// ── 그래프 섹션 ────────────────────────────────────────────────────────────

/**
 * 다크 배경 카드 + 타이틀 + Canvas 꺾은선 그래프.
 *
 * iOS: background #2C303E, cornerRadius 16, padding horizontal 20
 */
@Composable
private fun GraphSection(uiState: SeaAnalysisDetailUiState) {
    var selectedScale by remember { mutableStateOf(TemperatureGraphScale.DATE) }
    val graphDataSets = remember(uiState.surfaceValues, uiState.middleValues, uiState.bottomValues) {
        buildList {
            if (uiState.surfaceValues.isNotEmpty()) add(GraphDataSet(uiState.surfaceValues, SurfaceColor))
            if (uiState.middleValues.isNotEmpty())  add(GraphDataSet(uiState.middleValues, MiddleColor))
            if (uiState.bottomValues.isNotEmpty())  add(GraphDataSet(uiState.bottomValues, BottomColor))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(GraphBackground)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "최근 7일 수온 변화",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = selectedScale.subtitle,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                TemperatureGraphScaleMenu(
                    selectedScale = selectedScale,
                    onScaleSelected = { selectedScale = it }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White.copy(alpha = 0.7f))
                } else {
                    TemperatureLineGraph(
                        dataSets = graphDataSets,
                        dates = uiState.graphDates,
                        scale = selectedScale,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// ── Canvas 꺾은선 그래프 ───────────────────────────────────────────────────

/**
 * 그래프 데이터 세트 모델.
 * iOS의 GraphData struct에 대응합니다.
 */
data class GraphDataSet(
    val values: List<Float>,
    val color: Color
)

private enum class TemperatureGraphScale(
    val menuLabel: String,
    val subtitle: String,
    val intervalHours: Int?
) {
    DATE("날짜별", "일별 수온 추이 분석", null),
    HOURS_12("12시간", "12시간 단위 수온 추이 분석", 12),
    HOURS_6("6시간", "6시간 단위 수온 추이 분석", 6),
    HOURS_3("3시간", "3시간 단위 수온 추이 분석", 3);

    val isScrollable: Boolean
        get() = intervalHours != null
}

private data class ChartTick(
    val position: Float,
    val label: String
)

@Composable
private fun TemperatureGraphScaleMenu(
    selectedScale: TemperatureGraphScale,
    onScaleSelected: (TemperatureGraphScale) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = selectedScale.menuLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(116.dp)
        ) {
            TemperatureGraphScale.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.menuLabel,
                            fontSize = 14.sp,
                            fontWeight = if (option == selectedScale) FontWeight.Bold else FontWeight.Normal,
                            color = Color.Black
                        )
                    },
                    onClick = {
                        expanded = false
                        onScaleSelected(option)
                    },
                    modifier = Modifier.height(40.dp)
                )
            }
        }
    }
}

/**
 * Compose Canvas로 구현한 꺾은선 그래프.
 *
 * [개념] Canvas Composable은 DrawScope를 제공하며, 저수준 그래픽 API로 직접 그립니다.
 *        iOS의 TemperatureLineGraphView.swift (Path + GeometryReader) 로직을
 *        Compose DrawScope API로 1:1 이식했습니다.
 */
@Composable
private fun TemperatureLineGraph(
    dataSets: List<GraphDataSet>,
    dates: List<String>,
    scale: TemperatureGraphScale,
    modifier: Modifier = Modifier
) {
    val allValues = dataSets.flatMap { it.values }
    val validValues = allValues.filter { it > 0f }

    if (validValues.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "데이터가 없습니다.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
        return
    }

    val minTemp = floor(validValues.min().toDouble()).toFloat()
    val maxTemp = ceil(validValues.max().toDouble()).toFloat()
    val tempRange = (maxTemp - minTemp).coerceAtLeast(0.1f)

    val textMeasurer = rememberTextMeasurer()
    val horizontalScrollState = rememberScrollState()
    val pointCount = dataSets.maxOfOrNull { it.values.size } ?: 0
    val ticks = remember(scale, dates, pointCount) {
        buildChartTicks(scale = scale, dates = dates, pointCount = pointCount)
    }
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        lineHeight = 12.sp,
        color = Color.White.copy(alpha = 0.7f)
    )

    LaunchedEffect(scale) {
        horizontalScrollState.scrollTo(0)
    }

    Row(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
        ) {
            val h = size.height
            val topPad = 20f * density
            val bottomPad = 42f * density
            val graphH = h - topPad - bottomPad

            for (i in 0..4) {
                val ratio = i / 4f
                val y = (h - bottomPad) - (ratio * graphH)
                val tempLabel = String.format("%.1f", minTemp + tempRange * ratio)
                val measured = textMeasurer.measure(tempLabel, style = labelStyle)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(
                        x = 4f * density,
                        y = y - measured.size.height / 2f
                    )
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            val plotWidth = graphPlotWidth(
                scale = scale,
                pointCount = pointCount,
                availableWidth = maxWidth
            )
            val scrollModifier = if (scale.isScrollable) {
                Modifier.horizontalScroll(horizontalScrollState)
            } else {
                Modifier
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollModifier)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(plotWidth)
                        .fillMaxHeight()
                ) {
                    val w = size.width
                    val h = size.height
                    val topPad = 20f * density
                    val bottomPad = 42f * density
                    val graphH = h - topPad - bottomPad
                    val graphW = w

                    // ── 가로 가이드라인 ──────────────────────────────────────
                    for (i in 0..4) {
                        val ratio = i / 4f
                        val y = (h - bottomPad) - (ratio * graphH)
                        drawLine(
                            color = Color.White.copy(alpha = 0.3f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 0.5f * density
                        )
                    }

                    // ── 세로 가이드라인 + X축 라벨 ───────────────────────────
                    val labelY = h - bottomPad + 8f * density
                    ticks.forEach { tick ->
                        val x = tick.position * graphW
                        drawLine(
                            color = Color.White.copy(alpha = 0.12f),
                            start = Offset(x, topPad),
                            end = Offset(x, h - bottomPad),
                            strokeWidth = 0.5f * density
                        )

                        val measured = textMeasurer.measure(tick.label, style = labelStyle)
                        val maxLabelX = (w - measured.size.width).coerceAtLeast(0f)
                        val labelX = (x - measured.size.width / 2f).coerceIn(0f, maxLabelX)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(labelX, labelY)
                        )
                    }

                    // ── 데이터 꺾은선 ────────────────────────────────────────
                    dataSets.forEach { dataSet ->
                        if (dataSet.values.size < 2) return@forEach
                        val lineXStep = graphW / (dataSet.values.size - 1).toFloat()
                        val path = Path()
                        var pathStarted = false

                        dataSet.values.forEachIndexed { index, value ->
                            val x = index * lineXStep
                            if (value > 0f) {
                                val ratio = (value - minTemp) / tempRange
                                val y = (h - bottomPad) - (ratio * graphH)
                                if (!pathStarted) {
                                    path.moveTo(x, y)
                                    pathStarted = true
                                } else {
                                    path.lineTo(x, y)
                                }
                            } else {
                                if (pathStarted) {
                                    drawPath(
                                        path = path,
                                        color = dataSet.color,
                                        style = Stroke(
                                            width = 2f * density,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                    path.reset()
                                    pathStarted = false
                                }
                            }
                        }
                        if (pathStarted) {
                            drawPath(
                                path = path,
                                color = dataSet.color,
                                style = Stroke(
                                    width = 2f * density,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun graphPlotWidth(
    scale: TemperatureGraphScale,
    pointCount: Int,
    availableWidth: Dp
): Dp {
    if (!scale.isScrollable || pointCount <= 1) return availableWidth

    val pointSpacing = when (scale) {
        TemperatureGraphScale.HOURS_12 -> 2.65f.dp
        TemperatureGraphScale.HOURS_6 -> 5.3f.dp
        TemperatureGraphScale.HOURS_3 -> 9.dp
        TemperatureGraphScale.DATE -> 0.dp
    }
    val width = (pointSpacing.value * (pointCount - 1)).dp
    return if (width < availableWidth) availableWidth else width
}

private fun buildChartTicks(
    scale: TemperatureGraphScale,
    dates: List<String>,
    pointCount: Int
): List<ChartTick> {
    if (pointCount <= 1) return emptyList()

    if (scale == TemperatureGraphScale.DATE) {
        if (dates.isEmpty()) return emptyList()
        return dates.mapIndexed { index, date ->
            ChartTick(
                position = if (dates.size == 1) 0f else index / (dates.size - 1).toFloat(),
                label = date
            )
        }
    }

    val intervalHours = scale.intervalHours ?: return emptyList()
    val step = intervalHours * 2
    return buildList {
        var index = 0
        while (index < pointCount) {
            val dayOffset = index / 48
            if (dayOffset in dates.indices) {
                val hour = (index % 48) / 2
                val label = if (hour == 0) {
                    "${dates[dayOffset]}\n00시"
                } else {
                    String.format("%02d시", hour)
                }
                add(
                    ChartTick(
                        position = index / (pointCount - 1).toFloat(),
                        label = label
                    )
                )
            }
            index += step
        }
    }
}

// ── 수온 카드 섹션 ────────────────────────────────────────────────────────

/**
 * 표층/중층/저층 수온 카드를 3열로 배치.
 *
 * [개념] height(IntrinsicSize.Max) + fillMaxHeight()로 모든 카드의 높이를
 *        가장 큰 카드에 맞춰 동일하게 만듭니다. iOS LazyVGrid가 동일 행 높이를
 *        자동으로 맞추는 것과 동일한 효과입니다.
 *
 * Figma: 카드 height=157.46dp, outer margin 16dp, gap 12dp
 */
@Composable
private fun TemperatureCardsSection(uiState: SeaAnalysisDetailUiState) {
    val hasAnyData = uiState.hasSurfaceData || uiState.hasMiddleData || uiState.hasBottomData

    if (hasAnyData) {
        // [개념] 항상 3개의 weight(1f) 슬롯을 배치합니다.
        //        데이터가 없는 슬롯은 빈 Box로 채워 표층만 있어도 카드가
        //        항상 1/3 너비를 유지하도록 합니다.
        //        iOS LazyVGrid의 3열 GridItem(.flexible())과 동일한 고정 비율입니다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 슬롯 1 — 표층
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (uiState.hasSurfaceData) {
                    TemperatureCardView(
                        modifier = Modifier.fillMaxSize(),
                        type = "표층",
                        temp = uiState.surfaceTemp,
                        depth = uiState.surfaceDepth,
                        maxTemp = uiState.surfaceMax,
                        minTemp = uiState.surfaceMin,
                        accentColor = SurfaceColor
                    )
                }
            }
            // 슬롯 2 — 중층
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (uiState.hasMiddleData) {
                    TemperatureCardView(
                        modifier = Modifier.fillMaxSize(),
                        type = "중층",
                        temp = uiState.middleTemp,
                        depth = uiState.middleDepth,
                        maxTemp = uiState.middleMax,
                        minTemp = uiState.middleMin,
                        accentColor = MiddleColor
                    )
                }
            }
            // 슬롯 3 — 저층
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (uiState.hasBottomData) {
                    TemperatureCardView(
                        modifier = Modifier.fillMaxSize(),
                        type = "저층",
                        temp = uiState.bottomTemp,
                        depth = uiState.bottomDepth,
                        maxTemp = uiState.bottomMax,
                        minTemp = uiState.bottomMin,
                        accentColor = BottomColor
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "⚠", fontSize = 32.sp, color = SubtitleGray.copy(alpha = 0.5f))
                Text(text = "수온 데이터가 존재하지 않습니다.", fontSize = 15.sp, color = SubtitleGray)
            }
        }
    }
}

/**
 * 개별 수온 카드.
 *
 * Figma 스펙:
 * - 배경: White, radius 16dp
 * - 카드 padding: 16dp 전체
 * - 색상 바: 4dp × 16dp, radius 50%
 * - 수층명: 13sp SemiBold, #1F2937
 * - 온도: 24sp Bold, accentColor (softWrap=false로 개행 방지)
 * - 수심: 12sp SemiBold, accentColor
 * - 최고/최저: 11sp Medium, #6B7280, 행간 2dp
 */
@Composable
private fun TemperatureCardView(
    modifier: Modifier = Modifier,
    type: String,
    temp: String,
    depth: String,
    maxTemp: String,
    minTemp: String,
    accentColor: Color
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 수층 타입 레이블 (색상 바 + 텍스트)
        // Figma: y=16, h=19.5 — 카드 상단 16dp 패딩 후 배치
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accentColor)
            )
            Text(
                text = type,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )
        }

        // Figma gap: type label bottom(35.5) → temp top(47.5) = 12dp
        Spacer(modifier = Modifier.height(12.dp))

        // 현재 온도: Figma 24sp Bold, h=36dp
        // [개념] softWrap = false는 줄바꿈을 완전히 금지합니다.
        Text(
            text = temp,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )

        // Figma gap: temp bottom(83.5) → depth top(83.5) = 0dp (간격 없음)

        // 수심: Figma 12sp SemiBold, h=15dp
        Text(
            text = depth,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentColor
        )

        // Figma gap: depth bottom(98.5) → maxmin top(106.5) = 8dp
        Spacer(modifier = Modifier.height(8.dp))

        // 최고/최저: Figma 11sp Medium, #6B7280, 행간 2dp
        // [개념] fontSize/color/platformStyle을 하나의 TextStyle로 통합합니다.
        //        fontSize/color를 Text의 별도 파라미터로 넘기면 style.merge() 과정에서
        //        platformStyle(includeFontPadding=false)이 제대로 전파되지 않을 수 있습니다.
        //        모든 속성을 단일 TextStyle에 담아야 font padding 제거가 확실히 적용됩니다.
        val statTextStyle = TextStyle(
            fontSize = 11.sp,
            color = Color(0xFF6B7280),
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // [개념] alignByBaseline()은 Row 내 Text들을 폰트 기준선(baseline)에 맞춰 정렬합니다.
                //        한글과 숫자는 동일한 fontSize라도 글리프 메트릭(ascent/descent)이 달라
                //        CenterVertically만으로는 시각적으로 y가 어긋날 수 있습니다.
                //        기준선 정렬을 쓰면 두 텍스트가 동일한 텍스트 라인에 앉습니다.
                Text(text = "최고", style = statTextStyle, modifier = Modifier.alignByBaseline())
                Text(text = maxTemp, style = statTextStyle, modifier = Modifier.alignByBaseline())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "최저", style = statTextStyle, modifier = Modifier.alignByBaseline())
                Text(text = minTemp, style = statTextStyle, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

// ── 하단 안내 문구 ────────────────────────────────────────────────────────

/**
 * 데이터 제공 현황 안내 박스.
 *
 * Figma 스펙 (노드 406:2200):
 * - 배경: rgba(59, 130, 246, 0.1) = blue-500 10%
 * - 텍스트: 13sp Medium, #1E40AF (blue-800)
 * - 패딩: 16dp 전체 (top/bottom/left/right)
 * - 외부 margin: 16dp horizontal
 * - radius: 16dp
 * - 텍스트 정렬: start (좌측 정렬)
 */
@Composable
private fun FooterSection(uiState: SeaAnalysisDetailUiState) {
    val message = buildFooterMessage(
        hasSurface = uiState.hasSurfaceData,
        hasMiddle = uiState.hasMiddleData,
        hasBottom = uiState.hasBottomData
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FooterBgColor)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // [개념] Modifier.fillMaxWidth()로 텍스트가 박스 좌측부터 시작합니다.
        //        iOS의 .frame(maxWidth: .infinity) + default leading alignment에 대응합니다.
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = FooterTextColor
        )
    }
}

/**
 * 데이터 보유 조합에 따라 안내 문구를 반환합니다.
 * iOS의 footerMessage 연산 프로퍼티에 대응합니다.
 */
private fun buildFooterMessage(
    hasSurface: Boolean,
    hasMiddle: Boolean,
    hasBottom: Boolean
): String = when {
    hasSurface && hasMiddle && hasBottom   -> "이 지역은 표층, 중층, 저층 모든 수온 데이터를 제공합니다."
    hasSurface && hasMiddle && !hasBottom  -> "이 지역은 표층, 중층 수온 데이터만 제공됩니다."
    hasSurface && !hasMiddle && !hasBottom -> "이 지역은 표층 수온 데이터만 제공됩니다."
    hasSurface && !hasMiddle && hasBottom  -> "이 지역은 표층, 저층 수온 데이터만 제공됩니다."
    !hasSurface && hasMiddle && hasBottom  -> "이 지역은 중층, 저층 수온 데이터만 제공됩니다."
    !hasSurface && hasMiddle && !hasBottom -> "이 지역은 중층 수온 데이터만 제공됩니다."
    !hasSurface && !hasMiddle && hasBottom -> "이 지역은 저층 수온 데이터만 제공됩니다."
    else                                   -> "이 지역의 수온 데이터가 없습니다."
}
