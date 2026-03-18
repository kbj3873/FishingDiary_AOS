package com.onbada.seathermo.presentation.onboarding.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.onbada.seathermo.common.utils.AppPreferences
import com.onbada.seathermo.common.utils.PreferenceConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 온보딩 화면의 상태와 페이지 진행을 관리하는 ViewModel.
 *
 * [개념] AndroidViewModel은 내부적으로 Application Context를 참조할 수 있는 ViewModel입니다.
 * Context가 필요한 유틸리티(예: SharedPreferences) 사용 시 유용합니다.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    // 현재 보고 있는 온보딩 페이지 인덱스를 보유하는 상태 스트림.
    // [개념] StateFlow는 항상 최신 값을 보유하며, Compose UI에서 변경을 감지해 자동으로 다시 그립니다(Recomposition).
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    val totalPages = 4

    /**
     * 특정 페이지로 직접 이동 (HorizontalPager 스와이프 동기화용)
     *
     * [개념] Compose의 HorizontalPager가 스와이프로 페이지를 변경하면
     *        ViewModel의 currentPage도 동기화해야 마지막 페이지 판별이 정확해집니다.
     */
    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    /**
     * 다음 페이지로 이동하거나, 마지막 페이지면 온보딩 완료 처리
     *
     * [개념] 고차 함수(Higher-Order Function) 파라미터 onComplete: () -> Unit을 사용하여,
     * 함수 완료 후 외부(예: UI 컴포넌트)에 작업을 위임(Delegate)합니다.
     * Swift의 클로저(Closure)와 동일하게 동작합니다.
     */
    fun nextPage(onComplete: () -> Unit) {
        if (_currentPage.value < totalPages - 1) {
            _currentPage.value += 1
        } else {
            completeOnboarding()
            onComplete()
        }
    }

    /**
     * 온보딩 완료 플래그를 SharedPreferences에 저장
     */
    private fun completeOnboarding() {
        val context = getApplication<Application>()
        AppPreferences.setBoolean(context, PreferenceConstants.HAS_COMPLETED_ONBOARDING, true)
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                    return OnboardingViewModel(application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
