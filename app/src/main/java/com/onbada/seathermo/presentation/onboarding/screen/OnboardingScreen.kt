package com.onbada.seathermo.presentation.onboarding.screen

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onbada.seathermo.R
import com.onbada.seathermo.presentation.onboarding.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

// ── 색상 상수 ──────────────────────────────────────────────────────────────────
private val BackgroundColor    = Color(0xFFF2F2F7)  // Figma: #F2F2F7
private val PrimaryBlue        = Color(0xFF2563EB)  // Figma: #2563EB
private val TitleColor         = Color(0xFF1F2937)  // Figma: #1F2937
private val DescriptionColor   = Color(0xFF8E8E93)  // Figma: #8E8E93
private val InactiveDotColor   = Color(0xFFC7C7CC)  // Figma: #C7C7CC

// ── 페이지 데이터 모델 ──────────────────────────────────────────────────────────
private data class OnboardingPageData(
    val iconResId: Int,       // 원형 배경 안 아이콘 (80dp)
    val guideImageResId: Int, // 프리뷰 카드 이미지
    val title: String,
    val description: String
)

// ── 4개 페이지 데이터 ──────────────────────────────────────────────────────────
private val onboardingPages = listOf(
    OnboardingPageData(
        iconResId      = R.drawable.ic_onboarding_thermometer,
        guideImageResId = R.drawable.ic_onboarding_guide1,
        title          = "실시간 수온 확인",
        description    = "즐겨찾기한 지역의 표층, 중층, 저층\n수온을 실시간으로 확인하세요"
    ),
    OnboardingPageData(
        iconResId      = R.drawable.ic_onboarding_trending,
        guideImageResId = R.drawable.ic_onboarding_guide2,
        title          = "수온 변화 분석",
        description    = "서해, 동해, 남해 지역별로 최근 일주일\n간의 수온 변화를 분석하세요"
    ),
    OnboardingPageData(
        iconResId      = R.drawable.ic_onboarding_mappin,
        guideImageResId = R.drawable.ic_onboarding_guide3,
        title          = "GPS 낚시 기록",
        description    = "이동 경로와 속도를 자동으로 추적하\n고 조과물 사진과 함께 기록하세요"
    ),
    OnboardingPageData(
        iconResId      = R.drawable.ic_onboarding_history,
        guideImageResId = R.drawable.ic_onboarding_guide4,
        title          = "낚시 히스토리",
        description    = "과거 기록을 확인하고 최고의 낚시 포\n인트를 다시 찾아보세요"
    )
)

/**
 * 온보딩 화면 — 4개 페이지를 스와이프하며 앱 기능을 소개합니다.
 *
 * [개념] HorizontalPager는 Compose에서 ViewPager2에 해당하는 스와이프 페이지 컴포넌트입니다.
 *        PagerState로 현재 페이지를 추적하며, SwiftUI의 TabView(.page) 스타일과 동일합니다.
 *
 * @param viewModel   페이지 상태 및 온보딩 완료 처리를 담당하는 ViewModel
 * @param onNavigateToMain 온보딩 완료 시 메인 화면으로 이동하는 콜백
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onNavigateToMain: () -> Unit
) {
    // [개념] collectAsStateWithLifecycle()은 Flow를 Compose State로 변환합니다.
    //        화면이 백그라운드로 가면 수집을 자동 중단해 배터리를 절약합니다.
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()

    // [개념] rememberPagerState는 HorizontalPager의 현재 페이지와 스크롤 상태를 기억합니다.
    //        pageCount 람다로 총 페이지 수를 전달합니다.
    val pagerState = rememberPagerState(pageCount = { viewModel.totalPages })

    // [개념] rememberCoroutineScope()는 Composable의 생명주기에 묶인 코루틴 스코프를 반환합니다.
    //        버튼 클릭 같은 이벤트에서 suspend 함수를 호출할 때 사용합니다.
    val coroutineScope = rememberCoroutineScope()

    // ViewModel의 currentPage가 변경되면 Pager를 부드럽게 이동시킵니다.
    // [개념] LaunchedEffect(key)는 key 값이 바뀔 때마다 코루틴을 재실행합니다.
    //        Swift의 onChange(of: viewModel.currentPage) { }와 동일합니다.
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    // Pager 스와이프로 페이지가 바뀌면 ViewModel에도 동기화합니다.
    // [개념] snapshotFlow는 Compose State 변화를 Flow로 변환합니다.
    //        pagerState.currentPage가 변경될 때마다 ViewModel에 알립니다.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            viewModel.setCurrentPage(page)
        }
    }

    // [개념] Box는 SwiftUI의 ZStack과 동일하게 자식 뷰를 겹쳐 배치합니다.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // 페이지 콘텐츠 영역 — 하단 섹션을 제외한 나머지를 차지합니다.
            // [개념] Modifier.weight(1f)는 남은 공간을 모두 차지하도록 합니다.
            //        SwiftUI의 Spacer() + .frame(maxHeight: .infinity)와 동일합니다.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(page = onboardingPages[page])
            }

            // 하단 영역: 페이지 인디케이터 + 버튼
            OnboardingBottomSection(
                currentPage  = currentPage,
                totalPages   = viewModel.totalPages,
                onNext = {
                    coroutineScope.launch {
                        viewModel.nextPage(onComplete = onNavigateToMain)
                    }
                }
            )
        }
    }
}

/**
 * 개별 온보딩 페이지 콘텐츠.
 * 원형 아이콘 → 가이드 이미지 → 타이틀 → 설명 순으로 세로 배치합니다.
 */
@Composable
private fun OnboardingPageContent(page: OnboardingPageData) {
    // [개념] Column은 SwiftUI의 VStack과 동일합니다. verticalArrangement로 정렬 방식을 지정합니다.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── 원형 아이콘 배경 ───────────────────────────────────────────────
        // Figma: 128dp 원형, 배경 #2563EB, 내부 아이콘 80dp
        // [개념] Box + clip(CircleShape)으로 원형 컨테이너를 만듭니다.
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = page.iconResId),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Fit,
                // [개념] colorFilter로 이미지를 단색으로 tint합니다.
                //        iOS의 .renderingMode(.template) + .foregroundColor(.white)와 동일합니다.
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── 가이드 프리뷰 이미지 ────────────────────────────────────────────
        // Figma: 313dp 너비, 200dp 높이, scaledToFit
        androidx.compose.foundation.Image(
            painter = painterResource(id = page.guideImageResId),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )

        // 그림자가 시각적 여백을 추가하므로 Figma 32dp → 24dp로 조정
        Spacer(modifier = Modifier.height(16.dp))

        // ── 타이틀 ────────────────────────────────────────────────────────
        // Figma: 28sp bold, #1F2937, letterSpacing 0.38sp
        Text(
            text = page.title,
            style = TextStyle(
                fontSize     = 28.sp,
                fontWeight   = FontWeight.Bold,
                color        = TitleColor,
                letterSpacing = 0.38.sp,
                lineHeight   = 42.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── 설명 ──────────────────────────────────────────────────────────
        // Figma: 15sp medium, #8E8E93, lineHeight 24sp, letterSpacing -0.23sp, center
        Text(
            text = page.description,
            style = TextStyle(
                fontSize      = 15.sp,
                fontWeight    = FontWeight.Medium,
                color         = DescriptionColor,
                letterSpacing = (-0.23).sp,
                lineHeight    = 24.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

/**
 * 하단 고정 영역 — 페이지 인디케이터 + 다음/시작하기 버튼.
 *
 * Figma: 전체 높이 121dp, 수평 패딩 24dp, 인디케이터↔버튼 간격 24dp
 */
@Composable
private fun OnboardingBottomSection(
    currentPage: Int,
    totalPages: Int,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()  // 시스템 네비게이션 바 높이만큼 하단 패딩 자동 적용
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),  // Figma 기준 버튼 하단 여백
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // ── 페이지 인디케이터 ─────────────────────────────────────────────
        PageIndicator(currentPage = currentPage, totalPages = totalPages)

        // ── 다음 / 시작하기 버튼 ─────────────────────────────────────────
        // Figma: 57dp 높이, #2563EB, radius 16dp, shadow rgba(37,99,235,0.3)
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(57.dp)
                // [개념] shadow Modifier로 버튼 하단에 파란 그림자를 추가합니다.
                .shadow(
                    elevation     = 12.dp,
                    shape         = RoundedCornerShape(16.dp),
                    ambientColor  = PrimaryBlue.copy(alpha = 0.3f),
                    spotColor     = PrimaryBlue.copy(alpha = 0.3f)
                ),
            shape  = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text(
                // 마지막 페이지면 "시작하기", 아니면 "다음"
                text  = if (currentPage == totalPages - 1) "시작하기" else "다음",
                style = TextStyle(
                    fontSize      = 17.sp,
                    fontWeight    = FontWeight.SemiBold,
                    color         = Color.White,
                    letterSpacing = (-0.43).sp
                )
            )
        }
    }
}

/**
 * 커스텀 페이지 인디케이터.
 * 활성 페이지는 캡슐(24×8dp), 비활성은 원(8×8dp)으로 표시하며 크기가 애니메이션됩니다.
 *
 * Figma: active #2563EB 24×8dp, inactive #C7C7CC 8×8dp, gap 8dp
 */
@Composable
private fun PageIndicator(currentPage: Int, totalPages: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalPages) { index ->
            val isActive = index == currentPage

            // [개념] animateDpAsState는 Dp 값이 변경될 때 부드러운 애니메이션을 적용합니다.
            //        SwiftUI의 .animation(.easeInOut, value:)와 동일한 효과입니다.
            val width by animateDpAsState(
                targetValue  = if (isActive) 24.dp else 8.dp,
                animationSpec = tween(durationMillis = 300),
                label        = "indicatorWidth"
            )

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (isActive) PrimaryBlue else InactiveDotColor)
            )
        }
    }
}
