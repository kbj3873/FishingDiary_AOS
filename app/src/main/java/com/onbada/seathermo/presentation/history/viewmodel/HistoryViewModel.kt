package com.onbada.seathermo.presentation.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.domain.entity.FishingRecord
import com.onbada.seathermo.domain.usecase.FishingRecordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 조과 기록 목록(History) 화면의 데이터를 관리하는 ViewModel.
 *
 * [개념] DB에 저장된 개별 위치 포인트들을 sessionId별로 그룹화하여 하나의 '낚시 세션' 아이템으로 변환합니다.
 *        iOS의 HistoryViewModel 로직을 Android 환경에 맞게 이식했습니다.
 */
class HistoryViewModel(
    private val useCase: FishingRecordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    /**
     * 전체 기록을 로드하고 그룹화합니다.
     */
    fun loadRecords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            try {
                // [개념] fetchAllRecords()는 Coroutine suspend 함수로 비동기적으로 데이터를 가져옵니다.
                val allRecords = useCase.fetchAllRecords()
                val historyItems = groupAndConvert(allRecords)
                
                _uiState.update { it.copy(
                    isLoading = false,
                    records = historyItems
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false, 
                    errorMessage = e.message
                )}
            }
        }
    }

    /**
     * 개별 포인트 리스트를 sessionId 기준으로 그룹화하여 UI용 아이템으로 변환합니다.
     */
    private fun groupAndConvert(records: List<FishingRecord>): List<HistoryRecordItem> {
        if (records.isEmpty()) return emptyList()

        // 1. sessionId별 그룹화
        val grouped = records.groupBy { it.sessionId }
        
        val items = grouped.mapNotNull { (sessionId, sessionRecords) ->
            if (sessionRecords.isEmpty()) return@mapNotNull null
            
            // 시간순 정렬
            val sorted = sessionRecords.sortedBy { it.date }
            val first = sorted.first()
            val last = sorted.last()
            
            // 출발 시간 포맷 (HH:mm 출발)
            val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTime = "${timeSdf.format(Date(first.date))} 출발"
            
            // 지점 수 계산 (상태 변경 횟수 + 사진 개수)
            var stateChangeCount = 0
            var lastState: Int? = null
            for (record in sorted) {
                if (lastState != null && lastState != record.state) {
                    stateChangeCount++
                }
                lastState = record.state
            }
            
            val photoCount = sorted.sumOf { it.imagePaths.size }
            val totalPointCount = stateChangeCount + photoCount
            
            // 소요 시간 계산
            val durationMs = last.date - first.date
            val hours = durationMs / (1000 * 60 * 60)
            val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)
            val duration = if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
            
            // 썸네일 경로 (첫 번째 사진)
            val thumbnailPath = sorted.firstOrNull { it.imagePaths.isNotEmpty() }?.imagePaths?.firstOrNull()

            HistoryRecordItem(
                id = if (sessionId.isEmpty()) UUID.randomUUID().toString() else sessionId,
                date = Date(first.date),
                startTime = startTime,
                pointCount = totalPointCount,
                photoCount = photoCount,
                duration = duration,
                thumbnailPath = thumbnailPath
            )
        }

        // 최신순 정렬
        return items.sortedByDescending { it.date }
    }

    companion object {
        /**
         * HistoryViewModel 생성을 위한 ViewModelProvider.Factory.
         *
         * [개념] companion object는 클래스에 종속된 정적 메서드/프로퍼티를 정의합니다.
         *        Swift의 static func와 동일합니다.
         *        ViewModelProvider.Factory는 UseCase처럼 기본 생성자 외 의존성이 필요한
         *        ViewModel을 생성할 때 반드시 필요합니다.
         */
        fun provideFactory(useCase: FishingRecordUseCase): ViewModelProvider.Factory {
            // [개념] object : 인터페이스 { } 는 익명 클래스(Anonymous Class)를 생성합니다.
            //        Swift에는 없는 개념으로, 인터페이스를 일회성으로 구현할 때 사용합니다.
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HistoryViewModel(useCase) as T
                }
            }
        }
    }
}

/**
 * UI 모델 정의.
 */
data class HistoryUiState(
    val records: List<HistoryRecordItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class HistoryRecordItem(
    val id: String,
    val date: Date,
    val startTime: String,
    val pointCount: Int,
    val photoCount: Int,
    val duration: String,
    val thumbnailPath: String?
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(date)
}
