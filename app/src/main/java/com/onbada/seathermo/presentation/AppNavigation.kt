package com.onbada.seathermo.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onbada.seathermo.application.di.ApplicationDIContainer
import com.onbada.seathermo.common.utils.AppPreferences
import com.onbada.seathermo.common.utils.PreferenceConstants
import com.onbada.seathermo.presentation.maintab.MainTabScreen
import com.onbada.seathermo.presentation.onboarding.screen.OnboardingScreen
import com.onbada.seathermo.presentation.seaanalysis.screen.SeaAnalysisDetailScreen
import com.onbada.seathermo.presentation.onboarding.viewmodel.OnboardingViewModel
import com.onbada.seathermo.presentation.splash.SplashScreen
import com.onbada.seathermo.presentation.splash.viewmodel.SplashViewModel

/**
 * 앱 전체 네비게이션 구조를 정의하는 최상위 Composable.
 *
 * [개념] NavHost는 화면 전환의 중심 허브입니다.
 *        각 화면은 고유한 route 문자열로 식별되며,
 *        navController.navigate("route")로 이동합니다.
 *        iOS의 switch currentStep { case .splash: ... case .main: ... } 구조에 대응합니다.
 *
 * [앱 흐름]
 *   splash → onboarding (최초 실행) → main
 *   splash → main (온보딩 완료 후)
 *
 * @param diContainer 모든 ViewModel 팩토리를 제공하는 DI 컨테이너
 */
@Composable
fun AppNavigation(diContainer: ApplicationDIContainer) {

    // [개념] rememberNavController()는 화면 전환 상태를 기억하는 컨트롤러입니다.
    val navController = rememberNavController()

    // 온보딩 완료 여부 확인에 사용할 Context
    // [개념] LocalContext.current는 현재 Composable이 속한 Android Context를 가져옵니다.
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        // ── Splash ──────────────────────────────────────────────────────────
        composable("splash") {
            val splashViewModel: SplashViewModel = viewModel(
                factory = diContainer.makeSplashViewModelFactory()
            )
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToMain = {
                    // 온보딩 완료 여부에 따라 목적지 분기
                    // [개념] 조건부 navigate: 온보딩을 한 번도 본 적 없으면 onboarding으로,
                    //        이미 완료했으면 main으로 바로 이동합니다.
                    val hasCompletedOnboarding = AppPreferences.getBoolean(
                        context,
                        PreferenceConstants.HAS_COMPLETED_ONBOARDING
                    )
                    val destination = if (hasCompletedOnboarding) "main" else "onboarding"

                    navController.navigate(destination) {
                        // [개념] popUpTo + inclusive = true 는 백스택에서 splash를 완전히 제거합니다.
                        //        뒤로가기 시 Splash로 돌아가지 않습니다.
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // ── Onboarding ──────────────────────────────────────────────────────
        composable("onboarding") {
            // [개념] OnboardingViewModel은 Application Context가 필요한 AndroidViewModel입니다.
            //        provideFactory()로 Application을 주입합니다.
            val onboardingViewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModel.provideFactory(
                    context.applicationContext as android.app.Application
                )
            )
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onNavigateToMain = {
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        // ── Main (BottomTab) ─────────────────────────────────────────────────
        composable("main") {
            MainTabScreen(
                diContainer = diContainer,
                onNavigateToSeaAnalysisDetail = { stationCode ->
                    // [개념] 인코딩 없이 stationCode를 그대로 route에 삽입합니다.
                    //        stationCode는 영문+숫자로 구성되어 별도 인코딩이 불필요합니다.
                    navController.navigate("sea_analysis_detail/$stationCode")
                }
            )
        }

        // ── SeaAnalysisDetail ────────────────────────────────────────────────
        // [개념] {stationCode}는 NavArgs 플레이스홀더입니다.
        //        backStackEntry.arguments?.getString("stationCode")로 값을 꺼냅니다.
        //        iOS의 NavigationLink(destination: SeaAnalysisDetailView(viewModel:))에 대응합니다.
        composable("sea_analysis_detail/{stationCode}") { backStackEntry ->
            val stationCode = backStackEntry.arguments?.getString("stationCode") ?: ""
            SeaAnalysisDetailScreen(
                stationCode = stationCode,
                diContainer = diContainer,
                onNavigateBack = {
                    // [개념] popBackStack()은 백스택에서 현재 화면을 제거하고 이전 화면으로 돌아갑니다.
                    //        iOS의 presentationMode.wrappedValue.dismiss()에 대응합니다.
                    navController.popBackStack()
                }
            )
        }
    }
}
