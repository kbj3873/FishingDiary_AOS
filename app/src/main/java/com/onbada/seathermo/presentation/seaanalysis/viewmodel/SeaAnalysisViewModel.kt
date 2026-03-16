package com.onbada.seathermo.presentation.seaanalysis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.common.extensions.DateUtils
import com.onbada.seathermo.domain.entity.SeaAnalysisQuery
import com.onbada.seathermo.domain.entity.WeeklyTemperature
import com.onbada.seathermo.domain.usecase.OceanUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 수온 분석(Sea Analysis) 메인 화면의 데이터를 관리하는 ViewModel.
 */
class SeaAnalysisViewModel(
    private val oceanUseCase: OceanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeaAnalysisUiState())
    val uiState: StateFlow<SeaAnalysisUiState> = _uiState.asStateFlow()

    /**
     * 특정 관측소의 수온 데이터를 API로부터 불러옵니다.
     *
     * @param gruNam 해역 코드 (기본값 "S" - 남해)
     * @param staCde 관측소 코드 (기본값 "223" - 완도 청산)
     */
    fun fetchTemperatureData(gruNam: String = "S", staCde: String = "223") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // [개념] DateUtils를 사용하여 조회 시작일과 종료일을 yyyy-MM-dd 형식으로 가져옵니다.
            val obsFrom = DateUtils.startTempDateString()
            val obsTo = DateUtils.endTempDateString()

            // [개념] SeaAnalysisQuery 엔티티를 생성하여 API 요청 파라미터를 구성합니다.
            val query = SeaAnalysisQuery(
                id = "risaInfo",
                gruNam = gruNam,
                useYn = "Y",
                staCde = staCde,
                dataCnt = "",
                ord = "1",
                ordType = "A",
                obsFrom = obsFrom,
                obsTo = obsTo
            )

            try {
                // [수정] OceanUseCase의 실제 인터페이스인 fetchTemperature(query)를 호출합니다.
                val result = oceanUseCase.fetchTemperature(query)
                processResponse(result)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    /**
     * API 응답 리스트를 가공하여 UI용 그래프 데이터와 카드 데이터로 변환합니다.
     */
    private fun processResponse(list: List<WeeklyTemperature>) {
        if (list.isEmpty()) {
            _uiState.update { it.copy(isLoading = false) }
            return
        }

        // 1. 날짜 기준 정렬 (iOS: list.sorted { $0.dateT < $1.dateT })
        val sortedList = list.sortedBy { it.dateT }

        // 2. 그래프용 데이터 추출 (표층, 중층, 저층)
        // [개념] map 함수는 리스트의 각 요소를 다른 형태로 변환하여 새 리스트를 만듭니다.
        //        Swift의 map { }과 동일한 고차 함수입니다.
        val surfaceValues = sortedList.map { it.wtrTempS.toFloat() }
        val middleValues = sortedList.map { it.wtrTempM.toFloat() }
        val bottomValues = sortedList.map { it.wtrTempB.toFloat() }

        // 3. X축 날짜 라벨 추출 (중복 제거된 날짜 리스트: M/d 형식)
        val uniqueDays = sortedList.map { it.dateT.toString().take(8) }.distinct().sorted()
        val graphDates = uniqueDays.map { dayStr ->
            // yyyyMMdd -> M/d 변환
            try {
                val month = dayStr.substring(4, 6).toInt()
                val day = dayStr.substring(6, 8).toInt()
                "$month/$day"
            } catch (e: Exception) {
                dayStr // 예외 시 원본 유지
            }
        }

        // 4. 최신 데이터로 카드 정보 업데이트
        val latest = sortedList.last()
        val surfaceTemp = String.format("%.1f°", latest.wtrTempS)
        val middleTemp = String.format("%.1f°", latest.wtrTempM)
        val bottomTemp = String.format("%.1f°", latest.wtrTempB)

        // 최종 상태 업데이트
        // [개념] update { } 블록 내부에서 copy()를 호출하여 불변(Immutable) 상태 객체를 안전하게 교체합니다.
        _uiState.update { 
            it.copy(
                isLoading = false,
                surfaceTemp = surfaceTemp,
                middleTemp = middleTemp,
                bottomTemp = bottomTemp,
                surfaceValues = surfaceValues,
                middleValues = middleValues,
                bottomValues = bottomValues,
                graphDates = graphDates
            )
        }
    }
}

/**
 * SeaAnalysis 화면의 전체 UI 상태를 정의하는 Data Class.
 *
 * [개념] Compose UI는 이 하나의 상태 객체만을 바라보고 화면을 그립니다. (Single Source of Truth)
 *        속성값이 하나라도 변경되면 관련 Composable만 지능적으로 재그려집니다(Recomposition).
 */
data class SeaAnalysisUiState(
    val stationName: String = "완도 청산",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    
    // 카드용 텍스트 상태
    val surfaceTemp: String = "0.0°",
    val middleTemp: String = "0.0°",
    val bottomTemp: String = "0.0°",
    
    // 그래프용 수치 리스트
    val surfaceValues: List<Float> = emptyList(),
    val middleValues: List<Float> = emptyList(),
    val bottomValues: List<Float> = emptyList(),
    
    // X축 라벨 리스트
    val graphDates: List<String> = emptyList()
)
