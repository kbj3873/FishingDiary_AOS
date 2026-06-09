package com.onbada.seathermo.presentation.seaanalysis.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.onbada.seathermo.R

private val ScreenBackground = Color(0xFFF2F2F7)
private val HeaderBackground = Color.White
private val HeaderDividerColor = Color(0xFFE5E5EA)
private val TitleColor = Color.Black
private val SubtitleColor = Color(0xFF8E8E93)
private val EmptyIconColor = Color(0xFF9CA3AF)
private val EmptyTitleColor = Color(0xFF6B7280)

/**
 * public 빌드의 수온분석 준비중 화면.
 *
 * internal 빌드는 SeaAnalysisTabNavHost를 통해 실제 수온분석 기능을 유지합니다.
 */
@Composable
fun PublicSeaAnalysisPreparingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        PublicSeaAnalysisHeader()
        PublicSeaAnalysisEmptyState(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun PublicSeaAnalysisHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "수온 분석",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TitleColor,
                letterSpacing = (-0.355).sp
            )
            Text(
                text = "주간 수온 데이터를 확인하세요",
                fontSize = 14.sp,
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

@Composable
private fun PublicSeaAnalysisEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_history_fish),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = EmptyIconColor
            )
            Text(
                text = "수온 분석 서비스는 준비 중입니다",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = EmptyTitleColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "더 안정적인 해양 데이터를 제공하기 위해\n정비하고 있습니다",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = EmptyIconColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
