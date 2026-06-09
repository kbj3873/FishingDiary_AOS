package com.onbada.seathermo.presentation.splash.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.BuildConfig
import com.onbada.seathermo.common.utils.AppPreferences
import com.onbada.seathermo.common.utils.PreferenceConstants
import com.onbada.seathermo.domain.usecase.SplashUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

sealed class SplashState {
    object Loading : SplashState()
    object ReadyToNavigate : SplashState()
    data class ForceUpdate(val message: String) : SplashState()
    data class OptionalUpdate(val message: String) : SplashState()
}

/**
 * Splash 화면의 비즈니스 로직과 상태를 관리하는 ViewModel.
 *
 * [개념] ViewModel은 화면 회전 등 구성 변경 시에도 데이터를 유지하며,
 * UI 관련 데이터를 생명주기를 고려하여 관리합니다.
 * iOS의 ObservableObject와 유사한 역할을 수행합니다.
 */
class SplashViewModel(
    application: Application,
    private val splashUseCase: SplashUseCase
) : AndroidViewModel(application) {

    // UI 상태를 외부에 노출하는 읽기 전용 스트림.
    // [개념] StateFlow는 항상 최신 값을 보유하는 '상태 홀더'입니다.
    //        내부에서는 MutableStateFlow로 값을 변경하고 외부에는 읽기 전용으로 노출하여 캡슐화를 유지합니다.
    //        iOS의 @Published 특성과 유사합니다.
    private val _state = MutableStateFlow<SplashState>(SplashState.Loading)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    // 최소 표시 시간 (밀리초)
    private val minimumDisplayDuration = 1500L

    // Splash 화면 초기화 및 병렬 작업 수행.
    // [개념] viewModelScope.launch는 ViewModel의 생명주기에 묶인 코루틴을 시작합니다.
    //        ViewModel 소멸 시 코루틴도 자동 취소되어 메모리 누수를 방지합니다.
    //        Swift의 Task 블록과 유사합니다.
    fun onAppear() {
        val context: Context = getApplication()
        val currentVersion = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }

        viewModelScope.launch {
            // [개념] supervisorScope는 자식 코루틴의 실패가 부모 Job으로 전파되는 것을 차단합니다.
            //        async { }는 기본적으로 예외 발생 시 부모 launch Job까지 실패시키는데,
            //        supervisorScope 안에 넣으면 Deferred에만 예외가 저장되어
            //        await()를 감싸는 try-catch에서 올바르게 잡을 수 있습니다.
            //        Swift는 Task 간 예외가 자동 격리되므로 이 처리가 불필요합니다.
            supervisorScope {
                // 최소 표시 시간 보장 및 병렬 API 호출
                // [개념] async는 결과를 반환하는 코루틴 빌더로, 여러 작업을 동시에 실행할 때 사용됩니다.
                //        반환된 Deferred 객체에 대해 await()를 호출하여 결과를 가져옵니다.
                val versionResultDeferred = async { splashUseCase.checkVersion(appVersion = currentVersion) }
                val delayDeferred = async { delay(minimumDisplayDuration) }

                if (BuildConfig.INTERNAL_BUILD) {
                    // 관측소 목록은 internal 기능(크롤링 현재수온/수온분석)에서만 사용합니다.
                    // public 빌드는 seathermo.com/api/regions 호출을 하지 않습니다.
                    try {
                        val regions = splashUseCase.fetchRegions()
                        // ALL_REGION_LIST: CrawlingOceanSelectScreen의 전체 지역 목록 선택용
                        AppPreferences.setToList(context, regions, PreferenceConstants.ALL_REGION_LIST)
                        println("[Splash] 관측소 목록 캐싱 완료: ${regions.size}개")
                    } catch (e: Exception) {
                        println("[Splash] 관측소 목록 fetch 실패: ${e.message}")
                    }
                }

                try {
                    val status = versionResultDeferred.await()
                    delayDeferred.await()

                    if (status.forceUpdate) {
                        _state.value = SplashState.ForceUpdate(message = status.message)
                    } else if (status.needUpdate) {
                        _state.value = SplashState.OptionalUpdate(message = status.message)
                    } else {
                        _state.value = SplashState.ReadyToNavigate
                    }
                } catch (e: Exception) {
                    delayDeferred.await()
                    // 버전 체크 실패 시 조용히 메인 화면으로 전환
                    println("[Splash] 버전 체크 실패: ${e.message}")
                    _state.value = SplashState.ReadyToNavigate
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            application: Application,
            splashUseCase: SplashUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
                    return SplashViewModel(application, splashUseCase) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
