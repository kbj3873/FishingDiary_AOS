package com.onbada.seathermo.presentation.fishingrecord.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.domain.entity.GeoPoint
import com.onbada.seathermo.domain.entity.MapLineInfo
import com.onbada.seathermo.domain.usecase.FishingRecordUseCase
import com.onbada.seathermo.managers.FDAppManager
import com.onbada.seathermo.managers.FDLocationManager
import com.onbada.seathermo.utility.LocationPermissionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 실시간 낚시 기록 화면의 데이터를 관리하는 ViewModel.
 *
 * [개념] 위치 추적, 속도 계산, 상태 판별 및 지점/사진 저장 로직을 총괄합니다.
 *        iOS의 FishingRecordViewModel 로직을 Android 환경에 맞게 이식했습니다.
 */
class FishingRecordViewModel(
    application: Application,
    private val useCase: FishingRecordUseCase
) : AndroidViewModel(application) {

    // UI 상태 통합 관리
    private val _uiState = MutableStateFlow(FishingRecordUiState())
    val uiState: StateFlow<FishingRecordUiState> = _uiState.asStateFlow()

    private val locationManager = FDLocationManager.getInstance(application)
    
    // 비동기 작업 관리용 (타이머 등)
    private var timerJob: Job? = null
    private var locationSubscriptionJob: Job? = null

    // 이전 상태 추적 및 스로틀링용
    private var lastFishingState: FDAppManager.FishingState? = null
    private var currentSessionId: String = ""
    private var lastSavedTime: Long = 0
    private val saveIntervalMs: Long = 3000 // 3초 간격 자동 저장

    /**
     * 위치 업데이트 구독 시작.
     * [개념] StateFlow.collectLatest는 최신 데이터가 오면 이전 처리를 취소하고 새 데이터를 받습니다.
     *        iOS의 Combine sink와 동일한 역할을 수행합니다.
     */
    fun startMonitoring() {
        locationSubscriptionJob?.cancel()
        locationSubscriptionJob = viewModelScope.launch {
            locationManager.currentMapLine.collectLatest { mapLine ->
                processLocationUpdate(mapLine)
            }
        }
    }

    fun stopMonitoring() {
        locationSubscriptionJob?.cancel()
    }

    /**
     * 위치 데이터 수신 시 속도 계산 및 상태 판별 로직을 실행합니다.
     */
    private fun processLocationUpdate(mapLine: MapLineInfo) {
        if (!_uiState.value.isRecording) return

        val currentLocation = mapLine.currentLocation
        val prevLocation = mapLine.previousLocation

        // 1. 속도 업데이트 (m/s -> knots)
        val speedMps = if (currentLocation.hasSpeed()) currentLocation.speed else 0f
        val speedKnots = speedMps * 1.94384 // knots 변환
        
        // 2. 낚시 상태 판별 (속도 기준)
        val newState = when {
            speedKnots >= FDAppManager.SPEED_THRESHOLD_HIGH -> FDAppManager.FishingState.MOVING
            speedKnots >= FDAppManager.SPEED_THRESHOLD_LOW -> FDAppManager.FishingState.DRIFTING
            else -> FDAppManager.FishingState.FISHING
        }

        // 3. 거리 계산 (이전 지점과의 거리 누적)
        var distanceDelta = 0.0
        if (prevLocation.latitude != 0.0 && prevLocation.longitude != 0.0) {
            distanceDelta = currentLocation.distanceTo(prevLocation).toDouble() / 1000.0 // km 단위
        }

        // 4. 상태 변경 감지 및 마커 추가
        if (lastFishingState != null && lastFishingState != newState) {
            // 변곡점(이전 좌표)에 마커 생성
            val markerCoord = if (prevLocation.latitude != 0.0) prevLocation else currentLocation
            val marker = StateChangeMarker(
                latitude = markerCoord.latitude,
                longitude = markerCoord.longitude,
                state = newState
            )
            
            _uiState.update { it.copy(
                markers = it.markers + marker,
                savedPointCount = it.savedPointCount + 1
            )}
            
            // 상태 변경 순간 지점 저장
            useCase.savePoint(
                sessionId = currentSessionId,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                speed = speedKnots.toDouble(),
                state = newState.value
            )
        }
        lastFishingState = newState

        // 5. 주기적 자동 저장 (3초 간격)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSavedTime >= saveIntervalMs) {
            useCase.savePoint(
                sessionId = currentSessionId,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                speed = speedKnots.toDouble(),
                state = newState.value
            )
            lastSavedTime = currentTime
        }

        // 6. UI 상태 최종 업데이트
        _uiState.update { it.copy(
            currentSpeed = speedKnots.toDouble(),
            fishingState = newState,
            distance = it.distance + distanceDelta,
            currentLocation = currentLocation,
            currentMapLine = mapLine
        )}
    }

    /**
     * 기록 시작.
     */
    fun startRecording() {
        if (!LocationPermissionHelper.hasLocationPermission(getApplication())) {
            _uiState.update { it.copy(isLocationPermissionPopupPresented = true) }
            return
        }

        currentSessionId = UUID.randomUUID().toString()
        lastFishingState = null
        lastSavedTime = System.currentTimeMillis()
        
        _uiState.update { it.copy(
            isRecording = true,
            markers = emptyList(),
            photoMarkers = emptyList(),
            distance = 0.0,
            duration = 0,
            savedPointCount = 1 // 시작 지점 포함
        )}

        locationManager.startTracking()
        startTimer()
        
        // 초기 시작 지점 저장
        locationManager.currentMapLine.value.currentLocation.let { loc ->
            useCase.savePoint(currentSessionId, loc.latitude, loc.longitude, 0.0, 0)
        }
    }

    /**
     * 기록 중단.
     */
    fun stopRecording() {
        // 종료 지점 저장
        val loc = _uiState.value.currentLocation ?: locationManager.currentMapLine.value.currentLocation
        useCase.savePoint(currentSessionId, loc.latitude, loc.longitude, 0.0, _uiState.value.fishingState.value)

        _uiState.update { it.copy(
            isRecording = false,
            isStopPopupPresented = false
        )}
        
        locationManager.stopTracking()
        stopTimer()
    }

    /**
     * 사진 저장.
     */
    fun savePhoto(imageData: ByteArray) {
        val loc = _uiState.value.currentLocation ?: return
        val geoPoint = GeoPoint(loc.latitude, loc.longitude)
        val state = _uiState.value.fishingState.value

        val path = useCase.savePhoto(currentSessionId, imageData, geoPoint, state)
        if (path != null) {
            val photoMarker = PhotoMarker(
                latitude = loc.latitude,
                longitude = loc.longitude,
                thumbnailPath = path
            )
            
            _uiState.update { it.copy(
                photoMarkers = it.photoMarkers + photoMarker,
                savedImagePaths = listOf(path) + it.savedImagePaths,
                savedPointCount = it.savedPointCount + 1
            )}
            
            // 사진 촬영 시 지점 명시적 저장
            useCase.savePoint(currentSessionId, loc.latitude, loc.longitude, _uiState.value.currentSpeed, state)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(duration = it.duration + 1) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun toggleSpeedUnit() {
        _uiState.update { it.copy(
            speedUnit = if (it.speedUnit == SpeedUnit.KNOTS) SpeedUnit.KMH else SpeedUnit.KNOTS
        )}
    }
    
    fun dismissPopups() {
        _uiState.update { it.copy(
            isStopPopupPresented = false,
            isLocationRationalePopupPresented = false,
            isLocationPermissionPopupPresented = false,
            isCameraRationalePopupPresented = false,
            isCameraPermissionPopupPresented = false
        )}
    }

    fun showStopPopup() {
        _uiState.update { it.copy(isStopPopupPresented = true) }
    }

    // [개념] 1차 거부 시 표시하는 Rationale 팝업 — 권한이 왜 필요한지 설명 후 재요청합니다.
    fun showLocationRationalePopup() {
        _uiState.update { it.copy(isLocationRationalePopupPresented = true) }
    }

    // [개념] 영구 거부 시 표시하는 팝업 — 시스템 설정으로 안내합니다.
    fun showLocationPermissionPopup() {
        _uiState.update { it.copy(isLocationPermissionPopupPresented = true) }
    }

    fun showCameraRationalePopup() {
        _uiState.update { it.copy(isCameraRationalePopupPresented = true) }
    }

    fun showCameraPermissionPopup() {
        _uiState.update { it.copy(isCameraPermissionPopupPresented = true) }
    }

    companion object {
        /**
         * ViewModel 생성 팩토리.
         *
         * [개념] AndroidViewModel은 Application 참조가 필요하므로
         *        기본 ViewModelProvider.Factory 대신 AndroidViewModelFactory를 상속합니다.
         *        iOS의 static func make() -> FishingRecordViewModel 패턴에 대응합니다.
         */
        fun provideFactory(
            application: Application,
            useCase: FishingRecordUseCase
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.AndroidViewModelFactory(application) {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return FishingRecordViewModel(application, useCase) as T
                }
            }
        }
    }
}

/**
 * UI 모델 및 상태 정의.
 */
data class FishingRecordUiState(
    val isRecording: Boolean = false,
    val fishingState: FDAppManager.FishingState = FDAppManager.FishingState.MOVING,
    val currentSpeed: Double = 0.0,
    val speedUnit: SpeedUnit = SpeedUnit.KNOTS,
    val distance: Double = 0.0,
    val duration: Long = 0,
    val currentLocation: Location? = null,
    val currentMapLine: MapLineInfo? = null,
    val markers: List<StateChangeMarker> = emptyList(),
    val photoMarkers: List<PhotoMarker> = emptyList(),
    val savedImagePaths: List<String> = emptyList(),
    val savedPointCount: Int = 0,
    val isStopPopupPresented: Boolean = false,
    // 위치 권한 1차 거부 시 — 재요청 안내 팝업
    val isLocationRationalePopupPresented: Boolean = false,
    // 위치 권한 영구 거부 시 — 시스템 설정 안내 팝업
    val isLocationPermissionPopupPresented: Boolean = false,
    // 카메라 권한 1차 거부 시 — 재요청 안내 팝업
    val isCameraRationalePopupPresented: Boolean = false,
    // 카메라 권한 영구 거부 시 — 시스템 설정 안내 팝업
    val isCameraPermissionPopupPresented: Boolean = false
) {
    val convertedSpeed: Double
        get() = if (speedUnit == SpeedUnit.KNOTS) currentSpeed else currentSpeed * 1.852
}

enum class SpeedUnit { KNOTS, KMH }

data class StateChangeMarker(
    val id: String = UUID.randomUUID().toString(),
    val latitude: Double,
    val longitude: Double,
    val state: FDAppManager.FishingState
)

data class PhotoMarker(
    val id: String = UUID.randomUUID().toString(),
    val latitude: Double,
    val longitude: Double,
    val thumbnailPath: String
)
