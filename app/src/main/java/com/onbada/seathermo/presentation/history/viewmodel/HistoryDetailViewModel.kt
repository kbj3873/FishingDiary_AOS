package com.onbada.seathermo.presentation.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.domain.entity.FishingRecord
import com.onbada.seathermo.domain.usecase.FishingRecordUseCase
import com.onbada.seathermo.managers.FDAppManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.location.Location

/**
 * 조과 기록 상세(History Detail) 화면의 데이터를 관리하는 ViewModel.
 *
 * [개념] 특정 낚시 세션의 경로(Polyline), 상태 변경 지점(State Markers), 사진(Photo Markers)을 분석하여 지도에 표시할 데이터로 변환합니다.
 *        iOS의 HistoryDetailViewModel 로직을 Android 환경에 맞게 이식했습니다.
 */
class HistoryDetailViewModel(
    private val sessionId: String,
    private val useCase: FishingRecordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

    /**
     * 화면 진입 시 해당 세션의 전체 데이터를 가져와 분석합니다.
     */
    fun fetchSessionData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // [개념] sessionId로 필터링된 기록을 가져옵니다.
                //        (실제로는 Repository 수준에서 필터링하는 것이 좋으나, iOS 로직에 맞춰 fetchAll 후 필터링 처리)
                val allRecords = useCase.fetchAllRecords()
                val sessionRecords = allRecords.filter { it.sessionId == sessionId }
                    .sortedBy { it.date }
                
                if (sessionRecords.isNotEmpty()) {
                    processRecords(sessionRecords)
                }
                
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    /**
     * 세션 기록들을 지도용 폴리라인, 마커 등으로 가공합니다.
     */
    private fun processRecords(records: List<FishingRecord>) {
        val first = records.first()
        val last = records.last()
        
        // 1. 요약 정보 설정
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateSdf = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        
        val durationMs = last.date - first.date
        val hours = durationMs / (1000 * 60 * 60)
        val minutes = (durationMs % (1000 * 60 * 60)) / (1000 * 60)
        val durationStr = if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
        
        val distance = calculateDistance(records)

        // 2. 지도 데이터 가공 (폴리라인 및 마커)
        val polylines = mutableListOf<HistoryPolyline>()
        val photoMarkers = mutableListOf<HistoryPhotoMarker>()
        val stateMarkers = mutableListOf<HistoryStateMarker>()
        
        var currentPath = mutableListOf<GeoCoord>()
        var currentState: Int? = null
        var lastState: Int? = null
        var pointIndex = 1

        for (record in records) {
            val coord = GeoCoord(record.location.latitude, record.location.longitude)
            val state = record.state
            val timeStr = timeSdf.format(Date(record.date))

            // 상태 변경 지점 마커 (이전과 상태가 달라지는 지점)
            if (lastState != null && lastState != state) {
                val markerState = when (state) {
                    0 -> FDAppManager.FishingState.MOVING
                    1 -> FDAppManager.FishingState.DRIFTING
                    else -> FDAppManager.FishingState.FISHING
                }
                
                stateMarkers.add(HistoryStateMarker(
                    id = UUID.randomUUID().toString(),
                    title = "지점 #$pointIndex",
                    timeString = timeStr,
                    latitude = coord.latitude,
                    longitude = coord.longitude,
                    state = markerState
                ))
                pointIndex++
            }
            lastState = state

            // 사진 마커
            if (record.imagePaths.isNotEmpty()) {
                for (path in record.imagePaths) {
                    photoMarkers.add(HistoryPhotoMarker(
                        id = UUID.randomUUID().toString(),
                        recordId = record.id,
                        latitude = coord.latitude,
                        longitude = coord.longitude,
                        thumbnailPath = path,
                        title = "지점 #$pointIndex",
                        timeString = timeStr
                    ))
                    pointIndex++
                }
            }

            // 폴리라인 세그먼트 (상태별 색상 분리)
            if (currentState == null) {
                currentState = state
                currentPath.add(coord)
            } else if (currentState == state) {
                currentPath.add(coord)
            } else {
                // 상태가 변하면 지금까지의 경로를 폴리라인으로 추가
                if (currentPath.size > 1) {
                    polylines.add(HistoryPolyline(currentPath.toList(), currentState))
                }
                // 이전 경로의 마지막 지점을 새 경로의 시작점으로 사용 (연결성 유지)
                currentPath = mutableListOf(currentPath.last(), coord)
                currentState = state
            }
        }
        
        // 마지막 남은 경로 추가
        if (currentPath.size > 1 && currentState != null) {
            polylines.add(HistoryPolyline(currentPath, currentState))
        }

        // 최종 상태 업데이트
        _uiState.update { it.copy(
            dateString = dateSdf.format(Date(first.date)),
            startTimeString = "${timeSdf.format(Date(first.date))} 출발",
            totalDuration = durationStr,
            totalDistance = distance,
            polylines = polylines,
            photoMarkers = photoMarkers,
            stateMarkers = stateMarkers,
            centerLatitude = first.location.latitude,
            centerLongitude = first.location.longitude
        )}
    }

    private fun calculateDistance(records: List<FishingRecord>): Double {
        if (records.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until records.size - 1) {
            val loc1 = Location("").apply {
                latitude = records[i].location.latitude
                longitude = records[i].location.longitude
            }
            val loc2 = Location("").apply {
                latitude = records[i + 1].location.latitude
                longitude = records[i + 1].location.longitude
            }
            total += loc1.distanceTo(loc2).toDouble()
        }
        return total / 1000.0 // km
    }

    fun deleteRecord() {
        useCase.deleteSession(sessionId)
        _uiState.update { it.copy(shouldDismiss = true) }
    }

    companion object {
        fun provideFactory(
            sessionId: String,
            useCase: FishingRecordUseCase
        ): androidx.lifecycle.ViewModelProvider.Factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HistoryDetailViewModel(sessionId, useCase) as T
            }
        }
    }
}

/**
 * UI 모델 정의.
 */
data class HistoryDetailUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val dateString: String = "",
    val startTimeString: String = "",
    val totalDistance: Double = 0.0,
    val totalDuration: String = "",
    val polylines: List<HistoryPolyline> = emptyList(),
    val photoMarkers: List<HistoryPhotoMarker> = emptyList(),
    val stateMarkers: List<HistoryStateMarker> = emptyList(),
    val centerLatitude: Double = 37.5665,
    val centerLongitude: Double = 126.9780,
    val shouldDismiss: Boolean = false
)

data class GeoCoord(val latitude: Double, val longitude: Double)

data class HistoryPolyline(
    val coordinates: List<GeoCoord>,
    val state: Int // 0: Moving, 1: Drifting, 2: Fishing
)

data class HistoryStateMarker(
    val id: String,
    val title: String,
    val timeString: String,
    val latitude: Double,
    val longitude: Double,
    val state: FDAppManager.FishingState
)

data class HistoryPhotoMarker(
    val id: String,
    val recordId: String,
    val latitude: Double,
    val longitude: Double,
    val thumbnailPath: String,
    val title: String,
    val timeString: String
)
