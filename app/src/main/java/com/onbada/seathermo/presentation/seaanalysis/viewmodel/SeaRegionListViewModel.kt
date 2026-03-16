package com.onbada.seathermo.presentation.seaanalysis.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.onbada.seathermo.common.utils.AppPreferences
import com.onbada.seathermo.common.utils.PreferenceConstants
import com.onbada.seathermo.domain.entity.Region
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 해역별 관측소 목록을 관리하는 ViewModel.
 *
 * [개념] 특정 해역(동해, 서해, 남해)을 기준으로 필터링된 지역 리스트를 로컬 저장소에서 가져옵니다.
 *        iOS의 SeaRegionListViewModel이 UserDefaults를 직접 사용한 패턴을
 *        Android의 AppPreferences를 사용하여 구현했습니다.
 *        Application context에 접근하기 위해 AndroidViewModel을 상속합니다.
 */
class SeaRegionListViewModel(
    application: Application,
    private val seaAreaId: String // "E" (동해), "W" (서해), "S" (남해)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SeaRegionListUiState())
    val uiState: StateFlow<SeaRegionListUiState> = _uiState.asStateFlow()

    init {
        // ViewModel 생성 시 제목 설정 및 데이터 로드
        val title = when (seaAreaId) {
            "E" -> "동해 지역"
            "W" -> "서해 지역"
            "S" -> "남해 지역"
            else -> "전체 지역"
        }
        _uiState.update { it.copy(title = title) }
        loadObservatories()
    }

    /**
     * 로컬 저장소(AppPreferences)에서 전체 지역 목록을 가져와 필터링합니다.
     */
    private fun loadObservatories() {
        // [개념] AppPreferences.getFromList는 iOS의 FDUserDefaults.getFromList와 동일한 역할을 합니다.
        //        ViewModel이 소유한 application context를 사용합니다.
        val allRegions = AppPreferences.getFromList<Region>(
            getApplication(), 
            PreferenceConstants.ALL_REGION_LIST
        )

        // [개념] toSea().id를 사용하여 전달받은 seaAreaId와 비교 필터링합니다.
        //        sortedBy { it.regionName }은 iOS의 sorted { $0.name < $1.name }에 대응합니다.
        val filteredRegions = allRegions
            .filter { it.toSea().id == seaAreaId }
            .sortedBy { it.regionName }

        _uiState.update { 
            it.copy(
                isLoading = false,
                observatories = filteredRegions
            )
        }
    }
}

/**
 * 지역 목록 화면의 UI 상태.
 */
data class SeaRegionListUiState(
    val title: String = "",
    val subtitle: String = "수온 데이터를 확인할 지역을 선택하세요",
    val isLoading: Boolean = false,
    val observatories: List<Region> = emptyList(),
    val errorMessage: String? = null
)
