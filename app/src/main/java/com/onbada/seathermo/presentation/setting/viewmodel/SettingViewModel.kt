package com.onbada.seathermo.presentation.setting.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.onbada.seathermo.domain.entity.MapType
import com.onbada.seathermo.managers.FDAppManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 앱 설정(Setting) 화면의 데이터를 관리하는 ViewModel.
 *
 * [개념] 지도 타입 선택, 버전 정보, 약관 링크 등 앱 전역 설정과 관련된 상태를 관리합니다.
 *        iOS의 SettingViewModel 로직을 Android 환경에 맞게 이식하고 확장했습니다.
 */
class SettingViewModel(
    private val appManager: FDAppManager = FDAppManager.getInstance()
) : ViewModel() {

    // UI 상태 통합 관리
    private val _uiState = MutableStateFlow(SettingUiState())
    val uiState: StateFlow<SettingUiState> = _uiState.asStateFlow()

    init {
        // 초기 상태 로드: AppManager의 현재 지도 타입을 반영합니다.
        _uiState.update { it.copy(
            selectedMapType = appManager.mapType
        )}
    }

    /**
     * 사용자가 선택한 지도 타입을 변경하고 AppManager에 저장합니다.
     *
     * 낚시 기록 진행 중이면 변경을 차단하고 팝업 플래그를 설정합니다.
     *
     * [개념] 지도 타입 변경은 앱 전역에 영향을 주는 설정이므로,
     *        ViewModel 내부 상태뿐 아니라 FDAppManager(Singleton)에도 즉시 반영합니다.
     *        Swift의 appManager.mapTp = type 할당 로직과 동일합니다.
     *
     * @param type 변경할 지도 타입 (Google Map / Kakao Map)
     */
    fun updateMapType(type: MapType) {
        if (type == _uiState.value.selectedMapType) return

        // 낚시 기록 진행 중이면 변경 차단 후 팝업 표시
        if (appManager.isRecording) {
            _uiState.update { it.copy(showRecordingBlockedDialog = true) }
            return
        }

        // 1. UI 상태 업데이트 (Composable이 즉시 재그려짐)
        _uiState.update { it.copy(selectedMapType = type) }

        // 2. 전역 매니저 상태 업데이트 (다른 화면 및 지도 컴포넌트에 반영)
        val rawValue = when (type) {
            MapType.GOOGLE_MAP -> 0
            MapType.KAKAO_MAP -> 1
        }
        appManager.setMapType(rawValue)
    }

    /**
     * 기록 중 지도 변경 차단 팝업을 닫습니다.
     */
    fun dismissRecordingBlockedDialog() {
        _uiState.update { it.copy(showRecordingBlockedDialog = false) }
    }

    /**
     * 캐시 삭제 등의 추가 설정 액션을 처리할 수 있는 메서드 (확장용).
     */
    fun clearCache() {
        // TODO: 캐시 삭제 로직 구현
    }

    companion object {
        /**
         * FDAppManager 싱글턴을 주입받는 커스텀 Factory.
         *
         * [개념] ViewModel 생성자에 파라미터를 전달할 때 ViewModelProvider.Factory가 필요합니다.
         *        DI 컨테이너(ApplicationDIContainer)를 통해 일관되게 생성합니다.
         *        iOS의 StateObject(wrappedValue: SettingViewModel(appManager:))에 대응합니다.
         */
        fun provideFactory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingViewModel() as T
            }
        }
    }
}

/**
 * Setting 화면의 전체 UI 상태를 정의하는 Data Class.
 *
 * [개념] Compose UI는 이 하나의 상태 객체만을 바라보고 화면을 그립니다.
 *        지도 타입 외에도 공지사항, 약관 등 메뉴 구성을 여기서 관리할 수 있습니다.
 */
data class SettingUiState(
    val selectedMapType: MapType = MapType.KAKAO_MAP,
    // 낚시 기록 중 지도 변경 시도 시 표시할 차단 팝업 플래그
    val showRecordingBlockedDialog: Boolean = false,
    val appVersion: String = "1.0.0", // TODO: BuildConfig에서 가져오도록 수정
    
    // 설정 메뉴 목록 (UI에서 리스트로 그릴 때 활용)
    val menuItems: List<SettingMenuItem> = listOf(
        SettingMenuItem.Notice,
        SettingMenuItem.TermsOfService,
        SettingMenuItem.PrivacyPolicy,
        SettingMenuItem.VersionInfo
    )
)

/**
 * 설정 메뉴 아이템 정의.
 */
sealed class SettingMenuItem(val title: String, val route: String) {
    object Notice : SettingMenuItem("공지사항", "setting/notice")
    object TermsOfService : SettingMenuItem("서비스 이용약관", "setting/terms")
    object PrivacyPolicy : SettingMenuItem("개인정보 처리방침", "setting/privacy")
    object VersionInfo : SettingMenuItem("버전 정보", "setting/version")
}
