package com.onbada.seathermo.presentation.seaanalysis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.common.extensions.DateUtils
import com.onbada.seathermo.domain.entity.Region
import com.onbada.seathermo.domain.entity.SeaAnalysisQuery
import com.onbada.seathermo.domain.entity.WeeklyTemperature
import com.onbada.seathermo.domain.usecase.OceanUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 수온 분석 상세(Sea Analysis Detail) 화면의 데이터를 관리하는 ViewModel.
 *
 * [개념] 7일간의 데이터를 30분 단위로 정규화하고, 누락된 데이터를 선형 보간(Linear Interpolation)하여
 *        부드러운 그래프를 생성하는 복잡한 UI 로직을 담당합니다.
 */
class SeaAnalysisDetailViewModel(
    private val oceanUseCase: OceanUseCase,
    private val region: Region
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeaAnalysisDetailUiState(stationName = region.regionName))
    val uiState: StateFlow<SeaAnalysisDetailUiState> = _uiState.asStateFlow()

    /**
     * 상세 수온 데이터를 불러오고 보간 로직을 실행합니다.
     */
    fun fetchTemperatureData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // [개념] DateUtils를 사용하여 조회 기간을 설정합니다.
            val obsFrom = DateUtils.startTempDateString()
            val obsTo = DateUtils.endTempDateString()

            // [개념] Region 엔티티의 toSea() 확장 함수를 사용하여 올바른 해역 ID를 가져옵니다.
            val query = SeaAnalysisQuery(
                id = "risaInfo",
                gruNam = region.toSea().id, 
                useYn = "Y",
                staCde = region.regionCode,
                dataCnt = "",
                ord = "1",
                ordType = "A",
                obsFrom = obsFrom,
                obsTo = obsTo
            )

            try {
                // [수정] 실제 인터페이스 fetchTemperature(query)를 호출합니다.
                val result = oceanUseCase.fetchTemperature(query)
                processResponse(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    /**
     * iOS의 복잡한 데이터 보간(Interpolation) 및 정규화 로직을 Android용으로 재구현합니다.
     */
    private fun processResponse(list: List<WeeklyTemperature>) {
        if (list.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        // 1. 날짜 기준 정렬
        val sortedList = list.sortedBy { it.dateT }
        
        // 2. 7일간 30분 단위 타임스탬프 생성 (337개 포인트)
        // [개념] Calendar.getInstance()는 현재 날짜와 시간을 가진 캘린더 객체를 생성합니다.
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // 오늘 00:00 기준으로 6일 전 (총 7일치 그래프)
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = calendar.time
        
        val expectedDates = mutableListOf<Date>()
        for (i in 0 until 337) {
            val dateCalendar = Calendar.getInstance().apply { 
                time = startDate
                add(Calendar.MINUTE, i * 30)
            }
            expectedDates.add(dateCalendar.time)
        }

        // 3. 응답 데이터를 Map으로 변환하여 조회 최적화
        val sdf = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
        val dataMap = sortedList.associateBy { 
            try { sdf.parse(it.dateT.toString()) } catch (e: Exception) { null }
        }

        val now = Date()

        /**
         * 선형 보간(Linear Interpolation) 헬퍼 함수
         */
        fun interpolate(
            dates: List<Date>, 
            selector: (WeeklyTemperature) -> Float
        ): List<Float> {
            val rawValues = dates.map { date ->
                val item = dataMap[date]
                if (item != null) selector(item) else null
            }.toMutableList()

            for (i in rawValues.indices) {
                if (rawValues[i] != null) continue

                // 미래 데이터는 숨김 처리 (-99.0)
                if (dates[i].after(now)) {
                    rawValues[i] = -99f
                    continue
                }

                // 이전/다음 유효 인덱스 찾기
                var prevIdx: Int? = null
                for (p in i - 1 downTo 0) {
                    if (rawValues[p] != null && rawValues[p]!! > -50f) {
                        prevIdx = p
                        break
                    }
                }

                var nextIdx: Int? = null
                for (n in i + 1 until rawValues.size) {
                    if (rawValues[n] != null && rawValues[n]!! > -50f) {
                        nextIdx = n
                        break
                    }
                    if (dates[n].after(now)) break
                }

                // 선형 보간 계산
                if (prevIdx != null && nextIdx != null) {
                    if ((nextIdx - prevIdx) > 24) {
                        rawValues[i] = -90f 
                    } else {
                        val pVal = rawValues[prevIdx]!!
                        val nVal = rawValues[nextIdx]!!
                        val steps = (nextIdx - prevIdx).toFloat()
                        val currentStep = (i - prevIdx).toFloat()
                        rawValues[i] = pVal + (nVal - pVal) * (currentStep / steps)
                    }
                } else if (prevIdx != null) {
                    rawValues[i] = if ((i - prevIdx) > 24) -90f else rawValues[prevIdx]
                } else if (nextIdx != null) {
                    rawValues[i] = if ((nextIdx - i) > 24) -90f else rawValues[nextIdx]
                } else {
                    rawValues[i] = -90f
                }
            }

            return rawValues.filterNotNull()
        }

        // 각 수층별 보간 실행
        val surfaceValues = interpolate(expectedDates) { it.wtrTempS }
        val middleValues = interpolate(expectedDates) { it.wtrTempM }
        val bottomValues = interpolate(expectedDates) { it.wtrTempB }

        // 4. X축 날짜 라벨 (7일치 M/d)
        val labelSdf = SimpleDateFormat("M/d", Locale.getDefault())
        val graphDates = (0 until 7).map { i ->
            val d = Calendar.getInstance().apply {
                time = startDate
                add(Calendar.DAY_OF_YEAR, i)
            }.time
            labelSdf.format(d)
        }

        // 5. 최고/최저 및 수심 정보
        val validSurface = surfaceValues.filter { it in -10f..50f }
        val lastValidSurface = sortedList.lastOrNull { it.wtrTempS > 0 }
        
        val validMiddle = middleValues.filter { it in -10f..50f }
        val lastValidMiddle = sortedList.lastOrNull { it.wtrTempM > 0 }

        val validBottom = bottomValues.filter { it in -10f..50f }
        val lastValidBottom = sortedList.lastOrNull { it.wtrTempB > 0 }

        _uiState.update { 
            it.copy(
                isLoading = false,
                hasSurfaceData = validSurface.isNotEmpty(),
                surfaceTemp = if (lastValidSurface != null) String.format("%.1f°", lastValidSurface.wtrTempS) else "-",
                surfaceMax = if (validSurface.isNotEmpty()) String.format("%.1f", validSurface.maxOrNull()) else "-",
                surfaceMin = if (validSurface.isNotEmpty()) String.format("%.1f", validSurface.minOrNull()) else "-",
                surfaceDepth = if (lastValidSurface != null && lastValidSurface.surDep > 0) "수심 ${lastValidSurface.surDep.toInt()}m" else "-",
                
                hasMiddleData = validMiddle.isNotEmpty(),
                middleTemp = if (lastValidMiddle != null) String.format("%.1f°", lastValidMiddle.wtrTempM) else "-",
                middleMax = if (validMiddle.isNotEmpty()) String.format("%.1f", validMiddle.maxOrNull()) else "-",
                middleMin = if (validMiddle.isNotEmpty()) String.format("%.1f", validMiddle.minOrNull()) else "-",
                middleDepth = if (lastValidMiddle != null && lastValidMiddle.midDep > 0) "수심 ${lastValidMiddle.midDep.toInt()}m" else "-",

                hasBottomData = validBottom.isNotEmpty(),
                bottomTemp = if (lastValidBottom != null) String.format("%.1f°", lastValidBottom.wtrTempB) else "-",
                bottomMax = if (validBottom.isNotEmpty()) String.format("%.1f", validBottom.maxOrNull()) else "-",
                bottomMin = if (validBottom.isNotEmpty()) String.format("%.1f", validBottom.minOrNull()) else "-",
                bottomDepth = if (lastValidBottom != null && lastValidBottom.botDep > 0) "수심 ${lastValidBottom.botDep.toInt()}m" else "-",

                surfaceValues = surfaceValues,
                middleValues = middleValues,
                bottomValues = bottomValues,
                graphDates = graphDates
            )
        }
    }

    companion object {
        /**
         * OceanUseCase와 Region을 주입받는 커스텀 Factory.
         *
         * [개념] ViewModel 생성자에 파라미터를 전달할 때 ViewModelProvider.Factory가 필요합니다.
         *        SeaRegionListViewModel.provideFactory와 동일한 패턴입니다.
         *        iOS의 StateObject(wrappedValue: SeaAnalysisDetailViewModel(region:)) 에 대응합니다.
         */
        fun provideFactory(
            oceanUseCase: OceanUseCase,
            region: Region
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SeaAnalysisDetailViewModel(oceanUseCase, region) as T
            }
        }
    }
}

/**
 * SeaAnalysis 상세 화면의 복합적인 UI 상태.
 */
data class SeaAnalysisDetailUiState(
    val stationName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    
    val hasSurfaceData: Boolean = false,
    val surfaceTemp: String = "-",
    val surfaceMax: String = "-",
    val surfaceMin: String = "-",
    val surfaceDepth: String = "-",
    
    val hasMiddleData: Boolean = false,
    val middleTemp: String = "-",
    val middleMax: String = "-",
    val middleMin: String = "-",
    val middleDepth: String = "-",
    
    val hasBottomData: Boolean = false,
    val bottomTemp: String = "-",
    val bottomMax: String = "-",
    val bottomMin: String = "-",
    val bottomDepth: String = "-",
    
    val surfaceValues: List<Float> = emptyList(),
    val middleValues: List<Float> = emptyList(),
    val bottomValues: List<Float> = emptyList(),
    val graphDates: List<String> = emptyList()
)
