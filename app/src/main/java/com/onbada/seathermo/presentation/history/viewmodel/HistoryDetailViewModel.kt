package com.onbada.seathermo.presentation.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.domain.entity.FishingRecord
import com.onbada.seathermo.domain.usecase.FishingRecordUseCase
import com.onbada.seathermo.managers.FDAppManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    // [개념] withContext(Dispatchers.Default)는 CPU 집약적 연산을 백그라운드 스레드에서 실행합니다.
                    //        수천 개 GPS 포인트의 루프 순회·폴리라인 세그먼트 생성이 메인 스레드를 블로킹하지 않도록
                    //        백그라운드로 이동합니다. iOS의 Task.detached(priority: .userInitiated)에 대응합니다.
                    withContext(Dispatchers.Default) {
                        processRecords(sessionRecords)
                    }
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

            // 상태 변경 지점 마커 (이동 → 탐색/낚시 전환 시에만 생성)
            // 마커 위치는 MOVING의 마지막 좌표(currentPath.last())에 찍어야 색상 전환 지점과 일치합니다.
            // coord는 이미 새 상태(DRIFTING/FISHING)의 첫 좌표이므로 사용하지 않습니다.
            if (lastState == 0 && state != 0) {
                val markerState = when (state) {
                    0 -> FDAppManager.FishingState.MOVING
                    1 -> FDAppManager.FishingState.DRIFTING
                    else -> FDAppManager.FishingState.FISHING
                }
                val markerCoord = if (currentPath.isNotEmpty()) currentPath.last() else coord

                stateMarkers.add(HistoryStateMarker(
                    id = UUID.randomUUID().toString(),
                    title = "지점 #$pointIndex",
                    timeString = timeStr,
                    latitude = markerCoord.latitude,
                    longitude = markerCoord.longitude,
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
            startMarker = endpointMarkerFor(
                record = first,
                id = "history_start_marker",
                title = "낚시 시작",
                timeString = timeSdf.format(Date(first.date))
            ),
            endMarker = endpointMarkerFor(
                record = last,
                id = "history_end_marker",
                title = "낚시종료",
                timeString = timeSdf.format(Date(last.date))
            ),
            centerLatitude = first.location.latitude,
            centerLongitude = first.location.longitude
        )}
    }

    private fun endpointMarkerFor(
        record: FishingRecord,
        id: String,
        title: String,
        timeString: String
    ): HistoryEndpointMarker? {
        val latitude = record.location.latitude
        val longitude = record.location.longitude
        if (latitude == 0.0 && longitude == 0.0) return null
        return HistoryEndpointMarker(
            id = id,
            title = title,
            timeString = timeString,
            latitude = latitude,
            longitude = longitude
        )
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

    /**
     * 마커 탭 시 호출. markerId로 어떤 마커가 선택됐는지 식별하여 selectedMarker를 업데이트합니다.
     * iOS의 mapView(_:didSelect:) 내 parent.selectedMarker 할당 로직에 대응합니다.
     */
    fun selectMarker(markerId: String) {
        val uiState = _uiState.value

        // 시작/종료 마커 검색 — 상태 마커와 같은 하단 카드 구조를 재사용합니다.
        val endpointMarker = listOfNotNull(uiState.startMarker, uiState.endMarker)
            .firstOrNull { it.id == markerId }
        if (endpointMarker != null) {
            _uiState.update { it.copy(
                selectedMarker = SelectedMarkerInfo(
                    title = endpointMarker.title,
                    timeString = endpointMarker.timeString,
                    thumbnailPath = null,
                    state = null
                )
            )}
            return
        }

        // 사진 마커 우선 검색
        val photoMarker = uiState.photoMarkers.firstOrNull { it.id == markerId }
        if (photoMarker != null) {
            _uiState.update { it.copy(
                selectedMarker = SelectedMarkerInfo(
                    title = photoMarker.title,
                    timeString = photoMarker.timeString,
                    thumbnailPath = photoMarker.thumbnailPath,
                    state = null
                )
            )}
            return
        }

        // 상태 마커 검색
        val stateMarker = uiState.stateMarkers.firstOrNull { it.id == markerId }
        if (stateMarker != null) {
            _uiState.update { it.copy(
                selectedMarker = SelectedMarkerInfo(
                    title = stateMarker.title,
                    timeString = stateMarker.timeString,
                    thumbnailPath = null,
                    state = stateMarker.state
                )
            )}
        }
    }

    /**
     * 하단 오버레이 카드를 닫습니다.
     * iOS의 viewModel.selectedMarker = nil에 대응합니다.
     */
    fun clearSelectedMarker() {
        _uiState.update { it.copy(selectedMarker = null) }
    }

    /**
     * 이미지 뷰어를 엽니다.
     * iOS의 viewModel.selectedImageIndex = index + isImageViewerPresented = true에 대응합니다.
     */
    fun openImageViewer(index: Int) {
        _uiState.update { it.copy(selectedImageIndex = index, isImageViewerPresented = true) }
    }

    /**
     * 이미지 뷰어를 닫습니다.
     * iOS의 presentationMode.dismiss()에 대응합니다.
     */
    fun closeImageViewer() {
        _uiState.update { it.copy(isImageViewerPresented = false) }
    }

    /**
     * 뷰어에서 현재 보이는 사진 인덱스를 업데이트합니다.
     * iOS의 viewModel.selectedImageIndex에 대응합니다.
     */
    fun setSelectedImageIndex(index: Int) {
        _uiState.update { it.copy(selectedImageIndex = index) }
    }

    /**
     * 이미지 뷰어에서 현재 표시 중인 사진을 삭제합니다.
     * iOS의 deletePhoto(at:)에 대응합니다.
     *
     * 삭제 후 남은 사진이 없으면 뷰어를 닫습니다.
     * iOS: markers.isEmpty → isImageViewerPresented = false
     */
    fun deletePhoto(at: Int) {
        val currentMarkers = _uiState.value.photoMarkers
        if (at < 0 || at >= currentMarkers.size) return

        val marker = currentMarkers[at]

        // [개념] deleteFishingRecord는 해당 record 전체(사진 포함)를 DB에서 삭제합니다.
        //        iOS의 useCase.deleteFishingRecord(id: marker.recordId)에 대응합니다.
        useCase.deleteFishingRecord(marker.recordId)

        // UI 목록에서 제거 + 데이터 변경 플래그
        val updatedMarkers = currentMarkers.toMutableList().also { it.removeAt(at) }

        val newIndex = when {
            updatedMarkers.isEmpty() -> 0
            at >= updatedMarkers.size -> updatedMarkers.size - 1
            else -> at
        }

        _uiState.update { it.copy(
            photoMarkers          = updatedMarkers,
            isDataModified        = true,
            // 남은 사진 없으면 뷰어 닫기, 있으면 인덱스 조정
            isImageViewerPresented = updatedMarkers.isNotEmpty(),
            selectedImageIndex    = newIndex
        )}
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
    val startMarker: HistoryEndpointMarker? = null,
    val endMarker: HistoryEndpointMarker? = null,
    val centerLatitude: Double = 37.5665,
    val centerLongitude: Double = 126.9780,
    val shouldDismiss: Boolean = false,
    // 마커 탭 시 하단 오버레이로 표시할 선택된 마커 정보.
    // iOS의 viewModel.selectedMarker 프로퍼티에 대응합니다.
    val selectedMarker: SelectedMarkerInfo? = null,
    // 이미지 뷰어 상태. iOS의 isImageViewerPresented / selectedImageIndex에 대응합니다.
    val isImageViewerPresented: Boolean = false,
    val selectedImageIndex: Int = 0,
    // 사진 삭제 등으로 데이터가 변경됐을 때 목록 갱신을 위한 플래그.
    // iOS의 isDataModified에 대응합니다.
    val isDataModified: Boolean = false
)

/**
 * 마커 탭 시 하단 오버레이 카드에 표시할 정보.
 * iOS의 HistoryDetailViewModel.SelectedMarkerInfo에 대응합니다.
 *
 * @param thumbnailPath null이면 상태 마커(stateMarkerCard), 아니면 사진 마커(photoMarkerCard)
 */
data class SelectedMarkerInfo(
    val title: String,
    val timeString: String,
    val thumbnailPath: String?,  // null → 상태 마커, non-null → 사진 마커
    val state: FDAppManager.FishingState?
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

data class HistoryEndpointMarker(
    val id: String,
    val title: String,
    val timeString: String,
    val latitude: Double,
    val longitude: Double
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
