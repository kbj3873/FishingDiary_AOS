package com.bj.fishingdiary.presentation.main

import android.content.Context
import com.bj.fishingdiary.application.AppConfiguration
import com.bj.fishingdiary.common.utils.FDUserDefaults
import com.bj.fishingdiary.common.utils.UserDefaultKey
import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.*
import com.bj.fishingdiary.domain.usecase.OceanUseCase
import com.bj.fishingdiary.domain.usecase.RisaListRequestValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 메인 화면 ViewModel
 * Main Screen ViewModel
 *
 * iOS의 MainViewModel.swift에 대응
 *
 * 역할:
 * - 수온 정보 데이터 로딩 및 관리
 * - 즐겨찾기한 수온 정보 필터링
 * - UI 상태 관리 (StateFlow)
 *
 * StateFlow vs iOS CurrentValueSubject:
 * - StateFlow: Kotlin Coroutines 기반 상태 관리
 * - CurrentValueSubject: Combine 기반 상태 관리
 * - 둘 다 현재 값을 유지하고 변경 시 구독자에게 알림
 *
 * @property context Application Context
 * @property appConfiguration 앱 전역 설정 (API 키 등)
 * @property oceanUseCase 해양 데이터 UseCase
 */
class MainViewModel(
    private val context: Context,
    private val appConfiguration: AppConfiguration,
    private val oceanUseCase: OceanUseCase
) {

    // ==================== 상태 관리 ====================

    /**
     * 수온 정보 리스트 StateFlow
     * Temperature information list StateFlow
     *
     * MutableStateFlow: 값을 변경할 수 있는 StateFlow (private)
     * StateFlow: 읽기 전용 StateFlow (public)
     *
     * iOS의 CurrentValueSubject<TempuratureListItemViewModel, Never>와 동일한 역할
     */
    private val _oceanStations = MutableStateFlow<List<OceanStationModel>>(emptyList())
    val oceanStations: StateFlow<List<OceanStationModel>> = _oceanStations.asStateFlow()

    /**
     * 로딩 상태 StateFlow
     * Loading state StateFlow
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 에러 메시지 StateFlow
     * Error message StateFlow
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * 현재 실행 중인 API 작업
     * Currently running API task
     *
     * 취소 가능하도록 참조 유지
     */
    private var oceanLoadTask: Cancellable? = null
        set(value) {
            // 새로운 작업이 시작되면 이전 작업 취소
            field?.cancel()
            field = value
        }

    // ==================== API 키 설정 ====================

    /**
     * RISA API 키
     *
     * AppConfiguration에서 가져옴
     * - local.properties에 RISA_API_KEY 설정
     * - build.gradle.kts에서 BuildConfig로 노출
     * - AppConfiguration에서 BuildConfig.RISA_API_KEY 접근
     */
    private val apiKeyRisa: String
        get() = appConfiguration.apiKeyRisa

    // ==================== 초기화 ====================

    /**
     * ViewModel 생성 시 자동 호출되는 초기화 블록
     */
    init {
        // 초기 데이터 로드는 Activity에서 명시적으로 호출하도록 함
        // iOS의 viewDidLoad()와 동일한 시점
    }

    // ==================== Public Methods ====================

    /**
     * 화면이 로드될 때 호출
     * Called when view is loaded
     *
     * iOS의 viewDidLoad()에 대응
     */
    fun viewDidLoad() {
        fetchRisaList()
    }

    /**
     * RISA 수온 목록 조회
     * Fetch RISA temperature list
     *
     * iOS의 fetchRisaList()와 동일
     */
    fun fetchRisaList() {
        val query = RisaListQuery(
            key = apiKeyRisa,
            id = "risaList",
            gruNam = "E"  // E: 동해, W: 서해, S: 남해
        )
        loadRisaList(query)
    }

    // ==================== Private Methods ====================

    /**
     * RISA 수온 목록 API 호출
     * Call RISA temperature list API
     *
     * iOS의 loadRisaList(risaListQuery:)와 동일
     */
    private fun loadRisaList(query: RisaListQuery) {
        _isLoading.value = true
        _errorMessage.value = null

        oceanLoadTask = oceanUseCase.executeRisaList(
            requestValue = RisaListRequestValue(query)
        ) { result ->
            _isLoading.value = false

            result.onSuccess { risaResponse ->
                // API 응답 성공
                val body = risaResponse.body
                val items = body?.item

                if (items.isNullOrEmpty()) {
                    println("no risa items")
                    _oceanStations.value = emptyList()
                    return@onSuccess
                }

                // 디버그 로그
                items.forEach { station ->
                    println("g:${station.gruNam} cd:${station.staCde} name:${station.staNamKor} obs:${station.obsLay} temp:${station.wtrTmp}")
                }

                // 모델 변환 및 필터링
                val visibleModel = makeModels(items)
                _oceanStations.value = loadSavedSeaTemperatureList(visibleModel)
            }

            result.onFailure { error ->
                // API 호출 실패
                println("Error: ${error.message}")
                _errorMessage.value = error.message
                _oceanStations.value = emptyList()
            }
        }
    }

    /**
     * RisaList를 OceanStationModel 리스트로 변환
     * Convert RisaList to OceanStationModel list
     *
     * iOS의 makeModels(_ items: [RisaList])와 동일
     *
     * 변환 로직:
     * 1. 중복 제거: staCde(관측소 코드) 기준으로 그룹화
     * 2. 각 관측소의 표층/중층/저층 수온 매핑
     * 3. 표층 수온 기준 오름차순 정렬
     */
    private fun makeModels(items: List<RisaList>): List<OceanStationModel> {
        // 1. 중복 제외한 관측소 코드 추출
        val uniqueStationCodes = items.map { it.staCde }.toSet()

        // 2. 각 관측소별 모델 생성
        val oceanStationList = uniqueStationCodes.map { code ->
            OceanStationModel(
                stationCode = code,
                stationName = "",
                surTemperature = "",
                midTemperature = "",
                botTemperature = ""
            )
        }.toMutableList()

        // 3. 각 관측소의 수온 데이터 채우기
        oceanStationList.forEachIndexed { index, model ->
            items.filter { it.staCde == model.stationCode }.forEach { item ->
                val updatedModel = when (item.obsLay) {
                    "1" -> model.copy(
                        surTemperature = item.wtrTmp,
                        stationName = item.staNamKor
                    )
                    "2" -> model.copy(
                        midTemperature = item.wtrTmp,
                        stationName = item.staNamKor
                    )
                    "3" -> model.copy(
                        botTemperature = item.wtrTmp,
                        stationName = item.staNamKor
                    )
                    else -> model.copy(stationName = item.staNamKor)
                }
                oceanStationList[index] = updatedModel
            }
        }

        // 4. 표층 수온 기준 오름차순 정렬
        return oceanStationList.sortedBy {
            it.surTemperature.toFloatOrNull() ?: 0f
        }
    }

    /**
     * 저장된 즐겨찾기 수온 정보만 필터링
     * Filter only saved favorite temperature information
     *
     * iOS의 loadSavedSeaTempuratureList와 동일
     *
     * @param allStations 전체 수온 정보 리스트
     * @return 즐겨찾기된 수온 정보 리스트 (제목 행 포함)
     */
    private fun loadSavedSeaTemperatureList(allStations: List<OceanStationModel>): List<OceanStationModel> {
        // SharedPreferences에서 즐겨찾기 목록 불러오기
        val favoriteStations = FDUserDefaults.getFromList<OceanStationModel>(
            context,
            UserDefaultKey.REGIONAL_SEA_TEMPERATURE_LIST
        )

        // 즐겨찾기된 항목만 필터링
        val visibleStations = allStations.filter { ocean ->
            favoriteStations.any { favorite ->
                ocean.stationCode == favorite.stationCode
            }
        }.toMutableList()

        // 제목 행 추가 (테이블 헤더)
        val titleModel = OceanStationModel(
            stationCode = "",
            stationName = "측정해역",
            surTemperature = "표층",
            midTemperature = "중층",
            botTemperature = "저층"
        )
        visibleStations.add(0, titleModel)

        return visibleStations
    }

    // ==================== Cleanup ====================

    /**
     * ViewModel이 제거될 때 호출
     * Called when ViewModel is cleared
     *
     * 리소스 정리 및 작업 취소
     */
    fun onCleared() {
        oceanLoadTask?.cancel()
    }
}
