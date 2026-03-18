package com.onbada.seathermo.presentation.currenttemperature.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.common.utils.AppPreferences
import com.onbada.seathermo.common.utils.PreferenceConstants
import com.onbada.seathermo.domain.entity.CombinedCurrentTemperature
import com.onbada.seathermo.domain.entity.Region
import com.onbada.seathermo.domain.entity.SeaAnalysisQuery
import com.onbada.seathermo.domain.entity.WeeklyTemperature
import com.onbada.seathermo.domain.usecase.OceanUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrawlingCurrentTemperatureUiState(
    val oceanStations: List<CombinedCurrentTemperature> = emptyList(),
    val isLoading: Boolean = false,
    val isOceanSelectPresented: Boolean = false
)

/**
 * 크롤링(웹 파싱) 기반 수온 정보를 관리하는 ViewModel.
 * 
 * [개념] ViewModel은 화면(Activity/Composable)이 사라져도 데이터를 유지하는 컨테이너입니다.
 * Android Jetpack의 lifecycle-viewmodel 라이브러리가 제공합니다.
 */
class CrawlingCurrentTemperatureViewModel(
    application: Application,
    private val oceanUseCase: OceanUseCase
) : AndroidViewModel(application) {

    // UI 상태를 노출하는 StateFlow
    // [개념] StateFlow는 항상 최신 값을 보유하는 '상태 홀더'입니다.
    //        초기값이 반드시 필요하며 상태가 변경되면 수집자(collector)에게 즉시 방출합니다.
    private val _uiState = MutableStateFlow(CrawlingCurrentTemperatureUiState())
    val uiState: StateFlow<CrawlingCurrentTemperatureUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        fetchStationList()
    }

    fun onAppear() {
        fetchStationList()
    }

    // [개념] 지역 추가/즐겨찾기 수정 후 화면에 돌아왔을 때 데이터 갱신을 위해 호출
    fun onDataUpdated() {
        fetchStationList()
    }

    /**
     * OceanSelectSheet 표시 여부를 변경합니다.
     * iOS의 isOceanSelectPresented @Published var 직접 대입에 대응합니다.
     */
    fun setOceanSelectPresented(isPresented: Boolean) {
        _uiState.update { it.copy(isOceanSelectPresented = isPresented) }
    }

    fun fetchStationList() {
        val context = getApplication<Application>()
        val favoriteRegions = AppPreferences.getFromList<Region>(
            context,
            PreferenceConstants.CRAWLING_FAVORITE_REGIONS
        )

        if (favoriteRegions.isEmpty()) {
            _uiState.update { it.copy(oceanStations = emptyList(), isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
            val todayDateString = dateFormat.format(Date())
            
            // [개념] Kotlin Coroutines의 async와 awaitAll은
            //        Swift의 TaskGroup(withTaskGroup) 처리와 정확히 대응됩니다.
            val deferredStations = favoriteRegions.map { region ->
                async {
                    val gruNam = toGruNam(region.regionGroup)
                    val query = SeaAnalysisQuery(
                        id = "risaInfo",
                        gruNam = gruNam,
                        useYn = "Y",
                        staCde = region.regionCode,
                        dataCnt = "1",
                        ord = "1",
                        ordType = "D",
                        obsFrom = todayDateString,
                        obsTo = todayDateString
                    )

                    try {
                        val weeklyTemps = oceanUseCase.fetchTemperature(query)
                        mapToCombined(region, weeklyTemps)
                    } catch (e: Exception) {
                        println("크롤링 오류 [${region.regionName}]: ${e.message}")
                        CombinedCurrentTemperature(
                            stationCode = region.regionCode,
                            stationName = region.regionName,
                            surTemperature = "",
                            midTemperature = "",
                            botTemperature = "",
                            seaName = region.regionGroup
                        )
                    }
                }
            }

            val results = deferredStations.awaitAll()
            
            // 즐겨찾기 순서 보존
            val ordered = favoriteRegions.mapNotNull { region ->
                results.firstOrNull { it.stationCode == region.regionCode }
            }

            _uiState.update { it.copy(oceanStations = ordered, isLoading = false) }
        }
    }

    private fun toGruNam(regionGroup: String): String {
        return when (regionGroup) {
            "동해" -> "E"
            "서해" -> "W"
            "남해" -> "S"
            "제주" -> "J"
            else -> regionGroup
        }
    }

    private fun mapToCombined(region: Region, weeklyTemps: List<WeeklyTemperature>): CombinedCurrentTemperature {
        val firstValidSurface = weeklyTemps.firstOrNull { it.wtrTempS > 0 }
        val firstValidMiddle = weeklyTemps.firstOrNull { it.wtrTempM > 0 }
        val firstValidBottom = weeklyTemps.firstOrNull { it.wtrTempB > 0 }

        return CombinedCurrentTemperature(
            stationCode = region.regionCode,
            stationName = region.regionName,
            surTemperature = firstValidSurface?.let { String.format(Locale.US, "%.1f", it.wtrTempS) } ?: "",
            midTemperature = firstValidMiddle?.let { String.format(Locale.US, "%.1f", it.wtrTempM) } ?: "",
            botTemperature = firstValidBottom?.let { String.format(Locale.US, "%.1f", it.wtrTempB) } ?: "",
            seaName = region.regionGroup
        )
    }

    companion object {
        fun provideFactory(
            application: Application,
            oceanUseCase: OceanUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(CrawlingCurrentTemperatureViewModel::class.java)) {
                    return CrawlingCurrentTemperatureViewModel(application, oceanUseCase) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
