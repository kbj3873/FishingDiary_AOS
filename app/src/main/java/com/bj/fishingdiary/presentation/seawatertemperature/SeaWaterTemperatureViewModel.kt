package com.bj.fishingdiary.presentation.seawatertemperature

import com.bj.fishingdiary.application.AppConfiguration
import com.bj.fishingdiary.common.extensions.DateUtils
import com.bj.fishingdiary.common.extensions.subStrings
import com.bj.fishingdiary.common.extensions.tempDateString
import com.bj.fishingdiary.common.extensions.toDate
import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.*
import com.bj.fishingdiary.domain.usecase.OceanRequestValue
import com.bj.fishingdiary.domain.usecase.OceanUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

/**
 * 수온 정보 화면 ViewModel
 * Sea Water Temperature Screen ViewModel
 *
 * iOS의 SeaWaterTemperatureViewModel.swift에 대응
 * Corresponds to SeaWaterTemperatureViewModel.swift in iOS
 *
 * 역할:
 * Role:
 * - 수온 데이터 조회 및 가공
 * - Query and process temperature data
 * - 더미 데이터 추가 (누락된 시간, 당일 00:00까지)
 * - Add dummy data (missing times, up to 00:00 of current day)
 * - UI 상태 관리
 * - Manage UI state
 *
 * @property appConfiguration 앱 전역 설정
 * @property oceanUseCase 해양 데이터 UseCase
 */
class SeaWaterTemperatureViewModel(
    private val appConfiguration: AppConfiguration,
    private val oceanUseCase: OceanUseCase
) {
    // ==================== 상태 관리 ====================

    /**
     * 수온 데이터 리스트
     * Temperature data list
     */
    private val _temperatureItems = MutableStateFlow<List<SeaWaterTemperatureSet>>(emptyList())
    val temperatureItems: StateFlow<List<SeaWaterTemperatureSet>> = _temperatureItems.asStateFlow()

    /**
     * 로딩 상태
     * Loading state
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 선택된 해역
     * Selected sea area
     */
    private val _selectedSea = MutableStateFlow(Sea.NONE)
    val selectedSea: StateFlow<Sea> = _selectedSea.asStateFlow()

    /**
     * 선택된 관측소
     * Selected observation station
     */
    private val _selectedObserv = MutableStateFlow<Observ?>(null)
    val selectedObserv: StateFlow<Observ?> = _selectedObserv.asStateFlow()

    // ==================== 작업 관리 ====================

    /**
     * 현재 실행 중인 데이터 로딩 작업
     * Current data loading task
     */
    private var oceanLoadTask: Cancellable? = null
        set(value) {
            field?.cancel()
            field = value
        }

    // ==================== Public Methods ====================

    /**
     * 해역 선택
     * Select sea area
     */
    fun selectSea(sea: Sea) {
        _selectedSea.value = sea
    }

    /**
     * 관측소 선택
     * Select observation station
     */
    fun selectObserv(observ: Observ?) {
        _selectedObserv.value = observ
    }

    /**
     * 수온 데이터 조회
     * Fetch temperature data
     *
     * @param gruNam 해역 그룹명 (예: "W", "E", "S")
     * @param staCde 관측소 코드
     */
    fun fetchTemperatureData(gruNam: String, staCde: String) {
        _isLoading.value = true

        val query = OceanQuery(
            id = "risaInfo",
            gruNam = gruNam,
            useYn = "Y",
            staCde = staCde,
            dataCnt = "",
            ord = "1",
            ordType = "A",
            obsFrom = DateUtils.startTempDateString(),
            obsTo = DateUtils.endTempDateString()
        )

        oceanLoadTask = oceanUseCase.execute(
            requestValue = OceanRequestValue(query)
                ) { result ->
                _isLoading.value = false
                result.onSuccess { oceanResponse ->
                    setItemsValue(oceanResponse.list)
                }
                result.onFailure { error ->
                    println("Error fetching temperature data: ${error.message}")
                    _temperatureItems.value = emptyList()
            }
        }
    }

    /**
     * 데이터 초기화
     * Clear data
     */
    fun clearData() {
        _temperatureItems.value = emptyList()
    }

    // ==================== Private Methods ====================

    /**
     * Ocean 리스트를 SeaWaterTemperatureSet으로 변환 및 가공
     * Convert and process Ocean list to SeaWaterTemperatureSet
     */
    private fun setItemsValue(oceanList: List<Ocean>) {
        var items = oceanList.map { ocean ->
            SeaWaterTemperatureSet(
                surTemp = ocean.wtrTempS,
                midTemp = ocean.wtrTempM,
                botTemp = ocean.wtrTempB,
                surDep = ocean.surDep,
                midDep = ocean.midDep,
                botDep = ocean.botDep,
                dateTime = ocean.dateT
            )
        }
        println("setItemsValue 1 : ${items}")
        // 시간순 정렬
        // Sort by time
        items = items.sortedBy { it.dateTime }
        println("setItemsValue 2 : ${items}")
        // 누락된 시간 더미 데이터 추가
        // Add dummy data for missing times
        items = addDummyData(items)
        println("setItemsValue 3 : ${items}")
        // 당일 00:00까지 더미 데이터 추가
        // Add dummy data up to 00:00 of current day
        items = addTodayDummyData(items)
        println("setItemsValue 4 : ${items}")
        _temperatureItems.value = items
    }

    /**
     * 중간에 누락된 시간의 더미 데이터 추가
     * Add dummy data for missing times in the middle
     *
     * 30분 간격으로 데이터가 있어야 하는데 누락된 경우, -90 값으로 채움
     * Fill with -90 value when data should exist at 30-minute intervals but is missing
     *
     * @param originItems 원본 데이터 리스트
     * @return 더미 데이터가 추가된 리스트
     */
    private fun addDummyData(originItems: List<SeaWaterTemperatureSet>): List<SeaWaterTemperatureSet> {
        if (originItems.isEmpty()) return originItems

        val items = originItems.toMutableList()
        var i = 0

        while (i < items.size - 1) {
            val temp = items[i]
            val date = temp.dateTime.toString().toDate() ?: continue

            val nextDate = items[i + 1].dateTime.toString().toDate() ?: continue

            // 30분 차이가 나는지 비교
            // Check if there's a 30-minute difference
            val diffMinutes = ((nextDate.time - date.time) / 1000 / 60).toInt()

            if (diffMinutes != 30) {
                // 30분 후 날짜 생성
                // Create date 30 minutes later
                val calendar = java.util.Calendar.getInstance()
                calendar.time = date
                calendar.add(java.util.Calendar.MINUTE, 30)

                val addedDate = calendar.time
                val dateTimeString = addedDate.tempDateString()
                val item = getDummyTempData(dateTimeString, -90f)
                items.add(i + 1, item)
            }

            i++
        }

        return items
    }

    /**
     * 당일 저녁 00:00까지 더미 데이터 추가
     * Add dummy data up to 00:00 of current day evening
     *
     * 그래프 날짜와 간격을 맞추기 위함
     * To align graph dates and intervals
     *
     * @param originItems 원본 데이터 리스트
     * @return 더미 데이터가 추가된 리스트
     */
    private fun addTodayDummyData(originItems: List<SeaWaterTemperatureSet>): List<SeaWaterTemperatureSet> {
        if (originItems.isEmpty()) return originItems

        val items = originItems.toMutableList()

        while (true) {
            val lastItem = items.last()
            val startDate = lastItem.dateTime.toString().toDate() ?: break

            // 30분 추가
            // Add 30 minutes
            val calendar = java.util.Calendar.getInstance()
            calendar.time = startDate
            calendar.add(java.util.Calendar.MINUTE, 30)

            val addedDate = calendar.time
            val dateTimeString = addedDate.tempDateString()
            val dateTimeLong = dateTimeString.toLongOrNull() ?: break

            val item = getDummyTempData(dateTimeString, -99f)

            // 자정(00:00)에 도달하면 종료
            // Stop when reaching midnight (00:00)
            if (dateTimeLong % 10000 == 0L) {
                break
            }

            items.add(item)
        }

        return items
    }

    /**
     * 더미 수온 데이터 생성
     * Create dummy temperature data
     *
     * @param dateTime 날짜 시간 문자열
     * @param value 온도 값 (-90: 중간 누락, -99: 당일 더미)
     * @return 더미 SeaWaterTemperatureSet
     */
    private fun getDummyTempData(dateTime: String, value: Float): SeaWaterTemperatureSet {
        return SeaWaterTemperatureSet(
            surTemp = value,
            midTemp = value,
            botTemp = value,
            surDep = value,
            midDep = value,
            botDep = value,
            dateTime = dateTime.toLongOrNull() ?: 0L
        )
    }
}
