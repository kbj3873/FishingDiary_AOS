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
 * 수온 즐겨찾기 화면 ViewModel
 * Ocean Favorites Screen ViewModel
 *
 * iOS의 OceanSelectViewModel.swift에 대응
 *
 * 역할:
 * - 전체 수온 관측소 목록 로딩
 * - 즐겨찾기 상태 관리
 * - 즐겨찾기 저장/삭제
 *
 * @property context Application Context
 * @property appConfiguration 앱 전역 설정 (API 키 등)
 * @property oceanUseCase 해양 데이터 UseCase
 */
class OceanSelectViewModel(
    private val context: Context,
    private val appConfiguration: AppConfiguration,
    private val oceanUseCase: OceanUseCase
) {

    // ==================== 상태 관리 ====================

    /**
     * 수온 관측소 리스트 StateFlow
     * Ocean station list StateFlow
     *
     * iOS의 CurrentValueSubject<[OceanStationModel], Never>와 동일한 역할
     */
    private val _items = MutableStateFlow<List<OceanStationModel>>(emptyList())
    val items: StateFlow<List<OceanStationModel>> = _items.asStateFlow()

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
     */
    private var oceanLoadTask: Cancellable? = null
        set(value) {
            field?.cancel()
            field = value
        }

    // ==================== API 키 설정 ====================

    /**
     * RISA API 키
     */
    private val apiKeyRisa: String
        get() = appConfiguration.apiKeyRisa

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

    /**
     * 즐겨찾기 체크 상태 저장/삭제
     * Save/remove favorite check state
     *
     * iOS의 saveCheckList(_ selected: Bool, model: OceanStationModel)와 동일
     *
     * @param selected 선택 여부 (true: 즐겨찾기 추가, false: 즐겨찾기 삭제)
     * @param model 대상 관측소 모델
     */
    fun saveCheckList(selected: Boolean, model: OceanStationModel) {
        // 1. SharedPreferences에서 저장된 즐겨찾기 목록 불러오기
        var savedOceanList = FDUserDefaults.getFromList<OceanStationModel>(
            context,
            UserDefaultKey.REGIONAL_SEA_TEMPERATURE_LIST
        ).toMutableList()

        // 2. 선택 상태에 따라 추가/삭제
        if (selected) {
            // 즐겨찾기 추가
            val exist = savedOceanList.any { it.stationCode == model.stationCode }
            if (!exist) {
                savedOceanList.add(model)
            }
        } else {
            // 즐겨찾기 삭제
            savedOceanList = savedOceanList.filter {
                it.stationCode != model.stationCode
            }.toMutableList()
        }

        // 3. SharedPreferences에 저장
        FDUserDefaults.setToList(
            context,
            savedOceanList,
            UserDefaultKey.REGIONAL_SEA_TEMPERATURE_LIST
        )

        // 4. UI 업데이트: 현재 리스트의 체크 상태 변경
        val updatedList = _items.value.map { item ->
            if (item.stationCode == model.stationCode) {
                item.copy(isChecked = selected)
            } else {
                item
            }
        }
        _items.value = updatedList
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
                    _items.value = emptyList()
                    return@onSuccess
                }

                // 디버그 로그
                items.forEach { station ->
                    println("g:${station.gruNam} cd:${station.staCde} name:${station.staNamKor} obs:${station.obsLay} temp:${station.wtrTmp}")
                }

                // 모델 변환 (체크 상태 포함)
                _items.value = makeModels(items)
            }

            result.onFailure { error ->
                // API 호출 실패
                println("Error: ${error.message}")
                _errorMessage.value = error.message
                _items.value = emptyList()
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
     * 3. 이전에 선택했던 관측소 체크 상태 반영
     * 4. 관측소 이름 기준 오름차순 정렬
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
                botTemperature = "",
                isChecked = false
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

        // 4. 이전에 선택했던 지역 체크
        val selectedOceanList = FDUserDefaults.getFromList<OceanStationModel>(
            context,
            UserDefaultKey.REGIONAL_SEA_TEMPERATURE_LIST
        )

        if (selectedOceanList.isNotEmpty()) {
            oceanStationList.forEachIndexed { index, model ->
                selectedOceanList.forEach { selectedOcean ->
                    if (model.stationCode == selectedOcean.stationCode) {
                        oceanStationList[index] = model.copy(isChecked = true)
                    }
                }
            }
        }

        // 5. 관측소 이름 기준 오름차순 정렬
        return oceanStationList.sortedBy { it.stationName }
    }

    // ==================== Cleanup ====================

    /**
     * ViewModel이 제거될 때 호출
     * Called when ViewModel is cleared
     */
    fun onCleared() {
        oceanLoadTask?.cancel()
    }
}
