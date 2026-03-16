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

/**
 * 해양 관측소 선택 화면의 비즈니스 로직을 관리하는 ViewModel.
 *
 * [개념] ViewModel은 UI와 비즈니스 로직 사이의 중재자 역할을 합니다.
 *        SharedPreferences를 사용하여 사용자의 선택한 관측소 목록을 유지합니다.
 */
class OceanSelectViewModel(
    application: Application,
    private val appConfiguration: AppConfiguration,
    private val oceanUseCase: OceanUseCase
) : AndroidViewModel(application) {

    // [개념] StateFlow는 상태가 업데이트될 때마다 수집 중인 UI에 새 값을 방출합니다.
    //        iOS의 @Published와 유사하게 리액티브한 UI 업데이트를 가능하게 합니다.
    private val _oceanStations = MutableStateFlow<List<CombinedCurrentTemperature>>(emptyList())
    val oceanStations: StateFlow<List<CombinedCurrentTemperature>> = _oceanStations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 데이터 변경 여부 추적
    private var isDataChanged: Boolean = false
    
    // 데이터 변경 시 호출될 콜백
    var onDataUpdated: (() -> Unit)? = null

    private var loadJob: Job? = null

    init {
        fetchRisaList()
    }

    /**
     * RISA API로부터 관측소 목록을 가져옵니다.
     */
    fun fetchRisaList() {
        _isLoading.value = true
        val query = CurrentTemperatureQuery(
            key = appConfiguration.apiKeyRisa,
            id = "risaList",
            gruNam = "E" // 기본적으로 동해 목록을 먼저 가져오거나 전체를 처리할 수 있습니다.
        )

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val response = oceanUseCase.fetchRisaList(query)
                val models = makeModels(response)
                _oceanStations.value = models
            } catch (e: Exception) {
                println("OceanSelectViewModel Error: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * API 응답 데이터를 UI 모델로 변환하고 즐겨찾기 상태를 병합합니다.
     */
    private fun makeModels(items: List<CurrentTemperature>): List<CombinedCurrentTemperature> {
        val oceanStationMap = mutableMapOf<String, CombinedCurrentTemperature>()

        for (item in items) {
            val code = item.staCde
            var model = oceanStationMap[code] ?: CombinedCurrentTemperature(
                stationCode = code,
                stationName = item.staNamKor,
                surTemperature = "",
                midTemperature = "",
                botTemperature = "",
                seaName = when (item.gruNam) {
                    "E" -> "동해"
                    "W" -> "서해"
                    "S" -> "남해"
                    "J" -> "제주"
                    else -> item.gruNam
                }
            )

            model = when (item.obsLay) {
                "1" -> model.copy(surTemperature = item.wtrTmp)
                "2" -> model.copy(midTemperature = item.wtrTmp)
                "3" -> model.copy(botTemperature = item.wtrTmp)
                else -> model
            }
            oceanStationMap[code] = model
        }

        val sortedList = oceanStationMap.values.toMutableList()
        
        // 이전에 선택했던 지역 체크
        val context = getApplication<Application>()
        val savedOceanList = AppPreferences.getFromList<CombinedCurrentTemperature>(
            context,
            PreferenceConstants.REGIONAL_SEA_TEMPERATURE_LIST
        )

        val finalItems = sortedList.map { model ->
            val isChecked = savedOceanList.any { it.stationCode == model.stationCode }
            model.copy(isChecked = isChecked)
        }.sortedBy { it.stationName }

        return finalItems
    }

    /**
     * 관측소 선택 상태를 변경하고 SharedPreferences에 저장합니다.
     *
     * [개념] copy() 메서드를 통해 불변 객체의 일부 속성만 변경된 새 객체를 생성합니다.
     */
    fun toggleSelection(model: CombinedCurrentTemperature, isSelected: Boolean) {
        val context = getApplication<Application>()
        var savedOceanList = AppPreferences.getFromList<CombinedCurrentTemperature>(
            context,
            PreferenceConstants.REGIONAL_SEA_TEMPERATURE_LIST
        ).toMutableList()

        if (isSelected) {
            if (savedOceanList.none { it.stationCode == model.stationCode }) {
                savedOceanList.add(model.copy(isChecked = true))
            }
        } else {
            savedOceanList.removeAll { it.stationCode == model.stationCode }
        }

        AppPreferences.setToList(context, savedOceanList, PreferenceConstants.REGIONAL_SEA_TEMPERATURE_LIST)

        // UI 상태 즉시 반영
        _oceanStations.update { currentList ->
            currentList.map { item ->
                if (item.stationCode == model.stationCode) {
                    item.copy(isChecked = isSelected)
                } else {
                    item
                }
            }
        }

        isDataChanged = true
    }

    /**
     * 화면이 닫힐 때 변경 사항이 있으면 콜백을 호출합니다.
     */
    fun onViewDisappear() {
        if (isDataChanged) {
            onDataUpdated?.invoke()
            isDataChanged = false
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
                if (modelClass.isAssignableFrom(OceanSelectViewModel::class.java)) {
                    return OceanSelectViewModel(application, appConfiguration, oceanUseCase) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
