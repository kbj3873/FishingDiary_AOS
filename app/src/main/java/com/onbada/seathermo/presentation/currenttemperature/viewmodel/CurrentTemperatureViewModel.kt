package com.onbada.seathermo.presentation.currenttemperature.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.application.AppConfiguration
import com.onbada.seathermo.common.utils.AppPreferences
import com.onbada.seathermo.common.utils.PreferenceConstants
import com.onbada.seathermo.domain.entity.CombinedCurrentTemperature
import com.onbada.seathermo.domain.entity.CurrentTemperature
import com.onbada.seathermo.domain.entity.CurrentTemperatureQuery
import com.onbada.seathermo.domain.usecase.OceanUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CurrentTemperatureUiState(
    val oceanStations: List<CombinedCurrentTemperature> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showErrorAlert: Boolean = false,
    val isOceanSelectPresented: Boolean = false
)

/**
 * 현재 수온 정보를 관리하는 메인 ViewModel.
 *
 * [개념] AndroidViewModel은 Application 컨텍스트가 필요한 경우 사용합니다.
 * SharedPreferences 접근 등 플랫폼 종속적인 기능 호출 시 유용합니다.
 */
class CurrentTemperatureViewModel(
    application: Application,
    private val appConfiguration: AppConfiguration,
    private val oceanUseCase: OceanUseCase
) : AndroidViewModel(application) {

    // UI 상태를 노출하는 StateFlow
    // [개념] MutableStateFlow로 내부에서 상태를 변경하고, UI에는 읽기 전용으로 노출합니다.
    //        Swift의 private(set) var와 동일한 의도입니다.
    private val _uiState = MutableStateFlow(CurrentTemperatureUiState())
    val uiState: StateFlow<CurrentTemperatureUiState> = _uiState.asStateFlow()

    private var allStationsCache: List<CombinedCurrentTemperature> = emptyList()
    private var loadJob: Job? = null

    init {
        // [개념] 초기화 시 즉시 캐시된 데이터를 화면에 보여주기 위해 호출합니다.
        refreshFilteredList()
        fetchStationList()
    }

    /**
     * 화면이 다시 포커스 받을 때 호출용
     */
    fun onAppear() {
        refreshFilteredList()
        fetchStationList()
    }

    /**
     * 즐겨찾기 데이터 변경 시 호출 (예: 지역 설정 후 되돌아왔을 때)
     */
    fun onDataUpdated() {
        refreshFilteredList()
        fetchStationList()
    }

    fun fetchStationList() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, showErrorAlert = false) }

        val query = CurrentTemperatureQuery(
            key = appConfiguration.apiKeyRisa,
            id = "risaList",
            gruNam = ""
        )

        loadJob?.cancel()
        // [개념] launch는 새로운 코루틴을 시작하며 즉시 반환됩니다. (Fire and forget)
        //        try-catch는 코루틴 내의 suspend 함수 파동에서 발생하는 예외를 안전하게 처리합니다.
        loadJob = viewModelScope.launch {
            try {
                val response = oceanUseCase.fetchRisaList(query)
                handleSuccess(response)
            } catch (e: Exception) {
                // [개념] Exception이 NetworkError 타입일 경우 추가 처리를 할 수 있지만,
                // 현재는 메세지 기반으로 구성합니다.
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showErrorAlert = true,
                        errorMessage = e.localizedMessage ?: "네트워크 요청에 실패했습니다."
                    )
                }
            }
        }
    }

    private fun handleSuccess(currentTemperatures: List<CurrentTemperature>) {
        val allStations = makeModels(currentTemperatures)
        allStationsCache = allStations

        val savedStations = filterSavedStations(allStations)
        _uiState.update {
            it.copy(oceanStations = savedStations, isLoading = false)
        }
    }

    private fun refreshFilteredList() {
        if (allStationsCache.isEmpty()) return
        val savedStations = filterSavedStations(allStationsCache)
        _uiState.update { it.copy(oceanStations = savedStations) }
    }

    private fun makeModels(currentTemperatures: List<CurrentTemperature>): List<CombinedCurrentTemperature> {
        val oceanStationList = mutableListOf<CombinedCurrentTemperature>()
        val seenCodes = mutableSetOf<String>()

        // 중복 관측소 코드 제거하여 기본 뼈대 생성
        for (item in currentTemperatures) {
            val code = item.staCde
            if (code !in seenCodes) {
                seenCodes.add(code)
                oceanStationList.add(
                    CombinedCurrentTemperature(
                        stationCode = code,
                        stationName = "",
                        surTemperature = "",
                        midTemperature = "",
                        botTemperature = "",
                        seaName = ""
                    )
                )
            }
        }

        // 수층별 데이터 매핑
        for (i in oceanStationList.indices) {
            var model = oceanStationList[i]
            for (item in currentTemperatures.filter { it.staCde == model.stationCode }) {
                model = when (item.obsLay) {
                    "1" -> model.copy(surTemperature = item.wtrTmp)
                    "2" -> model.copy(midTemperature = item.wtrTmp)
                    "3" -> model.copy(botTemperature = item.wtrTmp)
                    else -> model
                }
                model = model.copy(stationName = item.staNamKor)
            }
            oceanStationList[i] = model
        }

        return oceanStationList
    }

    private fun filterSavedStations(stations: List<CombinedCurrentTemperature>): List<CombinedCurrentTemperature> {
        val context = getApplication<Application>()
        val savedList = AppPreferences.getFromList<CombinedCurrentTemperature>(
            context,
            PreferenceConstants.REGIONAL_SEA_TEMPERATURE_LIST
        )
        return stations.filter { station ->
            savedList.any { saved -> saved.stationCode == station.stationCode }
        }
    }

    companion object {
        fun provideFactory(
            application: Application,
            appConfiguration: AppConfiguration,
            oceanUseCase: OceanUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CurrentTemperatureViewModel::class.java)) {
                    return CurrentTemperatureViewModel(application, appConfiguration, oceanUseCase) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
