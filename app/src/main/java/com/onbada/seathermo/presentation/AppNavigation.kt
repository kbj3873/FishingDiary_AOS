package com.onbada.seathermo.presentation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.onbada.seathermo.application.di.ApplicationDIContainer
import com.onbada.seathermo.presentation.maintab.MainTabScreen
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
 *   splash → main (온보딩은 추후 splash → onboarding → main 으로 확장 예정)
 *
 * @param diContainer 모든 ViewModel 팩토리를 제공하는 DI 컨테이너
 */
@Composable
fun AppNavigation(diContainer: ApplicationDIContainer) {

    // [개념] rememberNavController()는 화면 전환 상태를 기억하는 컨트롤러입니다.
    //        Recomposition 간 동일한 인스턴스를 유지합니다.
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {

        // ── Splash ──────────────────────────────────────────
        composable("splash") {
            // [개념] viewModel(factory = ...)은 DI 컨테이너의 팩토리로 ViewModel을 생성합니다.
            //        Compose Navigation의 각 composable 블록은 독립된 ViewModelStoreOwner이므로
            //        화면 단위로 ViewModel 생명주기가 관리됩니다.
            val splashViewModel: SplashViewModel = viewModel(
                factory = diContainer.makeSplashViewModelFactory()
            )
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToMain = {
                    // [개념] popUpTo + inclusive = true 는 백스택에서 splash를 완전히 제거합니다.
                    //        뒤로가기 시 Splash로 돌아가지 않도록 합니다.
                    //        iOS의 currentStep = .main 전환과 동일한 효과입니다.
                    navController.navigate("main") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // ── Main (BottomTab) ─────────────────────────────────
        // TODO: 온보딩 구현 후 splash → onboarding → main 순서로 확장
        composable("main") {
            MainTabScreen()
        }
    }
}
