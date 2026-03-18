package com.onbada.seathermo.presentation.seaanalysis.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onbada.seathermo.R
import com.onbada.seathermo.common.utils.AppPreferences
import com.onbada.seathermo.common.utils.PreferenceConstants
import com.onbada.seathermo.domain.entity.Region
import com.onbada.seathermo.domain.entity.Sea

/**
 * 수온 분석 메인 화면 (해역 선택).
 *
 * [개념] 이 화면은 ViewModel을 사용하지 않고 AppPreferences에서 직접 데이터를 읽어와 구성합니다.
 *        iOS의 SeaAnalysisView.swift 로직을 그대로 이식했습니다.
 */
@Composable
fun SeaAnalysisScreen() {
    val context = LocalContext.current

    // [개념] remember { } 블록 내부에서 데이터를 한 번만 읽어와 성능을 최적화합니다.
    //        iOS의 seaRegions 연산 프로퍼티 로직에 대응합니다.
    val seaRegions = remember {
        val allRegions = AppPreferences.getFromList<Region>(
            context,
            PreferenceConstants.ALL_REGION_LIST
        )

        // 해역별 개수 계산 (iOS와 동일 로직)
        val westCount = allRegions.count { it.toSea() == Sea.WEST }
        val eastCount = allRegions.count { it.toSea() == Sea.EAST }
        val southCount = allRegions.count { it.toSea() == Sea.SOUTH }

        listOf(
            SeaRegionInfo(
                id = Sea.WEST.id,
                title = "서해",
                subtitle = "황해 연안 지역",
                imageResId = R.drawable.img_sea_west,
                stationCount = westCount
            ),
            SeaRegionInfo(
                id = Sea.EAST.id,
                title = "동해",
                subtitle = "동해안 지역",
                imageResId = R.drawable.img_sea_east,
                stationCount = eastCount
            ),
            SeaRegionInfo(
                id = Sea.SOUTH.id,
                title = "남해",
                subtitle = "남해안 지역",
                imageResId = R.drawable.img_sea_south,
                stationCount = southCount
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // Figma: 배경색
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── 헤더 ────────────────────────────────────────────────────────
            SeaAnalysisHeader()

            // ── 카드 리스트 ──────────────────────────────────────────────────
            // [개념] LazyColumn은 iOS의 ScrollView + VStack(spacing: 8)에 대응합니다.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = seaRegions, key = { it.id }) { region ->
                    SeaRegionCardView(
                        region = region,
                        onClick = {
                            // TODO: 다음 단계에서 SeaRegionListScreen 연결
                            println("Selected Sea: ${region.title}")
                        }
                    )
                }
                // 하단 여백 확보 (iOS padding bottom 32dp 대응)
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// ── 헤더 섹션 ────────────────────────────────────────────────────────────────

@Composable
private fun SeaAnalysisHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp, bottom = 16.dp)
    ) {
        // 제목: Figma 30sp, Bold, Black, letterSpacing -0.355sp
        Text(
            text = "수온 분석",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = (-0.355).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 부제목: 14sp, Regular, #8E8E93, letterSpacing -0.15sp
        Text(
            text = "주간 수온 데이터를 확인하세요",
            fontSize = 14.sp,
            color = Color(0xFF8E8E93),
            letterSpacing = (-0.15).sp
        )
    }
    // 하단 구분선 — Figma: #E5E5EA, 0.5dp
    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE5E5EA))
}

// ── 해역 선택 카드 ────────────────────────────────────────────────────────────

@Composable
private fun SeaRegionCardView(
    region: SeaRegionInfo,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x0A000000),
                spotColor = Color(0x0A000000)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        // 1. 배경 이미지
        Image(
            painter = painterResource(id = region.imageResId),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. 가독성을 위한 하단 그라디언트 오버레이
        // Figma: from rgba(0,0,0,0.6) to transparent
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.0f),
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // 3. 텍스트 콘텐츠 (해역명 + 설명)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // 해역명: 32sp, Bold, White
            Text(
                text = region.title,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.09).sp
            )
            // 설명: 15sp, Medium, White 95%
            Text(
                text = region.subtitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.95f),
                letterSpacing = (-0.23).sp
            )
        }

        // 4. 지역 개수 배지 (우측 하단)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.25f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // "n개 지역": 13sp, SemiBold, White
                Text(
                    text = "${region.stationCount}개 지역",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    letterSpacing = (-0.07).sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                // 화살표 아이콘 (iOS 카드 디자인 참고)
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus), // ic_arrow_right 등이 없으면 우선 ic_plus 사용
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * 화면 구성을 위한 간단한 데이터 모델.
 */
private data class SeaRegionInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageResId: Int,
    val stationCount: Int
)
