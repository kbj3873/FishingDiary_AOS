package com.onbada.seathermo.presentation.currenttemperature.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.onbada.seathermo.common.utils.AppPreferences
import com.onbada.seathermo.common.utils.PreferenceConstants
import com.onbada.seathermo.domain.entity.CombinedCurrentTemperature
import com.onbada.seathermo.domain.entity.Region
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 크롤링 기반 수온 관측 지역 선택을 관리하는 ViewModel.
 *
 * [개념] 사용자가 즐겨찾는 지역을 최대 7개까지 선택할 수 있도록 제한 로직이 포함되어 있습니다.
 */
class CrawlingOceanSelectViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _oceanStations = MutableStateFlow<List<CombinedCurrentTemperature>>(emptyList())
    val oceanStations: StateFlow<List<CombinedCurrentTemperature>> = _oceanStations.asStateFlow()

    private val _showMaxAlert = MutableStateFlow(false)
    val showMaxAlert: StateFlow<Boolean> = _showMaxAlert.asStateFlow()

    private var isDataChanged: Boolean = false
    var onDataUpdated: (() -> Unit)? = null
    private val maxFavorites = 7

    init {
        loadRegions()
    }

    /**
     * 전체 지역 목록과 즐겨찾기 상태를 불러옵니다.
     */
    private fun loadRegions() {
        val context = getApplication<Application>()
        // TODO: 실제 프로젝트 상에서는 API 성공 시 전체 지역 목록을 저장해둔 allRegionList 키를 사용합니다.
        // 현재는 구현 상 필요한 키가 정의되어 있다고 가정합니다.
        val allRegions = AppPreferences.getFromList<Region>(context, PreferenceConstants.ALL_REGION_LIST)
        val favorites = AppPreferences.getFromList<Region>(context, PreferenceConstants.CRAWLING_FAVORITE_REGIONS)
        val favoriteCodes = favorites.map { it.regionCode }.toSet()

        _oceanStations.value = allRegions.map { region ->
            CombinedCurrentTemperature(
                stationCode = region.regionCode,
                stationName = region.regionName,
                surTemperature = "",
                midTemperature = "",
                botTemperature = "",
                seaName = region.regionGroup,
                isChecked = region.regionCode in favoriteCodes
            )
        }.sortedBy { it.stationName }
    }

    /**
     * 즐겨찾기 선택 상태를 전환합니다.
     * 
     * [개념] let 키워드는 객체가 null이 아닐 때 실행되는 블록을 정의합니다.
     */
    fun toggleSelection(model: CombinedCurrentTemperature, isSelected: Boolean) {
        val context = getApplication<Application>()
        val savedRegions = AppPreferences.getFromList<Region>(context, PreferenceConstants.CRAWLING_FAVORITE_REGIONS).toMutableList()

        if (isSelected) {
            if (savedRegions.size >= maxFavorites) {
                _showMaxAlert.value = true
                return
            }
            if (savedRegions.none { it.regionCode == model.stationCode }) {
                val allRegions = AppPreferences.getFromList<Region>(context, PreferenceConstants.ALL_REGION_LIST)
                allRegions.firstOrNull { it.regionCode == model.stationCode }?.let { region ->
                    savedRegions.add(region)
                }
            }
        } else {
            savedRegions.removeAll { it.regionCode == model.stationCode }
        }

        AppPreferences.setToList(context, savedRegions, PreferenceConstants.CRAWLING_FAVORITE_REGIONS)

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

    fun dismissMaxAlert() {
        _showMaxAlert.value = false
    }

    fun onViewDisappear() {
        if (isDataChanged) {
            onDataUpdated?.invoke()
            isDataChanged = false
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CrawlingOceanSelectViewModel::class.java)) {
                    return CrawlingOceanSelectViewModel(application) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
