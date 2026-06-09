package com.onbada.seathermo.presentation.fishingrecord.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.common.utils.AppPreferences
import com.onbada.seathermo.common.utils.PreferenceConstants
import com.onbada.seathermo.domain.entity.GeoPoint
import com.onbada.seathermo.domain.entity.MapLineInfo
import com.onbada.seathermo.domain.usecase.FishingRecordUseCase
import com.onbada.seathermo.managers.FDAppManager
import com.onbada.seathermo.managers.FDLocationManager
import com.onbada.seathermo.utility.LocationPermissionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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

    // 경로선 이벤트 스트림.
    // [개념] StateFlow 대신 SharedFlow를 사용하는 이유:
    //        경로선은 화면에서 LaunchedEffect로 직접 구독(collectAsStateWithLifecycle 미사용)합니다.
    //        LaunchedEffect의 코루틴은 앱이 백그라운드 상태여도 계속 실행되므로,
    //        백그라운드 중 발생한 모든 위치 이벤트를 누락 없이 지도에 그릴 수 있습니다.
    //        extraBufferCapacity = 500: 구독자가 일시 처리 지연 시 최대 500개 이벤트를 버퍼링합니다.
    //        Pair<MapLineInfo, FishingState>로 선분과 색상 정보를 함께 전달합니다.
    private val _mapLineEvents = MutableSharedFlow<Pair<MapLineInfo, FDAppManager.FishingState>>(
        extraBufferCapacity = 500
    )
    val mapLineEvents: SharedFlow<Pair<MapLineInfo, FDAppManager.FishingState>> = _mapLineEvents.asSharedFlow()

    private val locationManager = FDLocationManager.getInstance(application)
    
    // 비동기 작업 관리용 (타이머 등)
    private var timerJob: Job? = null
    private var locationSubscriptionJob: Job? = null

    // 이전 확정 상태 추적
    private var lastFishingState: FDAppManager.FishingState? = null
    private var currentSessionId: String = ""
    private var lastSavedTime: Long = 0
    private val saveIntervalMs: Long = 3000 // 3초 간격 자동 저장

    // [변경] 2m 거리 필터로 GPS 노이즈가 자연 억제되므로 debounce 없이 즉시 상태를 확정합니다.
    //        iOS FishingRecordViewModel과 동일한 방식입니다.

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
        val isCurrentLocationValid = currentLocation.latitude != 0.0 || currentLocation.longitude != 0.0
        val isPreviousLocationValid = prevLocation.latitude != 0.0 || prevLocation.longitude != 0.0

        // 1. 속도 업데이트 (m/s -> knots)
        val speedMps = if (currentLocation.hasSpeed()) currentLocation.speed else 0f
        val speedKnots = speedMps * 1.94384 // knots 변환
        
        // 2. 낚시 상태 판별 (속도 기준)
        val candidateState = when {
            speedKnots >= FDAppManager.SPEED_THRESHOLD_HIGH -> FDAppManager.FishingState.MOVING
            speedKnots >= FDAppManager.SPEED_THRESHOLD_LOW -> FDAppManager.FishingState.DRIFTING
            else -> FDAppManager.FishingState.FISHING
        }

        // 2m 거리 필터로 GPS 노이즈가 억제되므로 측정된 상태를 즉시 확정합니다.
        val confirmedState = candidateState

        // 3. 거리 계산 (이전 지점과의 거리 누적)
        var distanceDelta = 0.0
        if (prevLocation.latitude != 0.0 && prevLocation.longitude != 0.0) {
            distanceDelta = currentLocation.distanceTo(prevLocation).toDouble() / 1000.0 // km 단위
        }

        // 4. 상태 변경 감지 및 마커 추가 (확정된 상태 기준)
        // 이동(MOVING) → 탐색/낚시 전환 시에만 마커 생성 (불필요한 마커 최소화)
        if (lastFishingState == FDAppManager.FishingState.MOVING &&
            confirmedState != FDAppManager.FishingState.MOVING) {
            // prevLocation → currentLocation 세그먼트가 confirmedState 색으로 그려지므로
            // 마커는 해당 세그먼트의 시작점 prevLocation에 찍습니다.
            // prevLocation이 유효하지 않으면(0,0) currentLocation으로 폴백합니다.
            val markerLat = if (prevLocation.latitude != 0.0) prevLocation.latitude else currentLocation.latitude
            val markerLon = if (prevLocation.longitude != 0.0) prevLocation.longitude else currentLocation.longitude
            val marker = StateChangeMarker(
                latitude = markerLat,
                longitude = markerLon,
                state = confirmedState
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
                state = confirmedState.value
            )
        }
        lastFishingState = confirmedState

        // 5. 주기적 자동 저장 (3초 간격)
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSavedTime >= saveIntervalMs) {
            useCase.savePoint(
                sessionId = currentSessionId,
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                speed = speedKnots.toDouble(),
                state = confirmedState.value
            )
            lastSavedTime = currentTime
        }

        // 6. UI 상태 최종 업데이트 (확정된 상태 기준으로 경로 색상 및 UI 반영)
        _uiState.update { it.copy(
            currentSpeed = speedKnots.toDouble(),
            fishingState = confirmedState,
            distance = it.distance + distanceDelta,
            currentLocation = currentLocation,
            startLocation = it.startLocation ?: when {
                isPreviousLocationValid -> prevLocation
                isCurrentLocationValid -> currentLocation
                else -> null
            },
            endLocation = if (isCurrentLocationValid) currentLocation else it.endLocation
        )}

        // 7. 경로선 이벤트 발행 (지도 드로잉용 SharedFlow).
        // [개념] tryEmit은 버퍼에 공간이 있으면 즉시 emit하는 non-suspending 함수입니다.
        //        viewModelScope 코루틴 안에서 직접 emit 가능합니다.
        //        uiState 업데이트와 분리하여 백그라운드에서도 모든 경로 이벤트를 전달합니다.
        _mapLineEvents.tryEmit(mapLine to confirmedState)
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
            savedPointCount = 1, // 시작 지점 포함
            startLocation = null,
            endLocation = null
        )}

        // 전역 AppManager에 기록 시작 상태를 알립니다.
        // SettingViewModel이 기록 중 지도 타입 변경을 차단하는 데 사용합니다.
        FDAppManager.getInstance().setRecording(true)

        locationManager.startTracking()
        startTimer()

        // 초기 시작 지점 저장 — GPS가 확정된 경우에만 저장합니다.
        // [이유] locationManager의 초기값은 createDummyLocation(0.0, 0.0)으로,
        //        GPS 업데이트 수신 전에 읽으면 위도/경도 0.0(아프리카 기니만)이 저장됩니다.
        //        유효하지 않은 경우 건너뜁니다. 이후 processLocationUpdate에서
        //        3초 간격 자동 저장이 첫 실제 GPS 지점을 저장합니다.
        locationManager.currentMapLine.value.currentLocation.let { loc ->
            if (loc.latitude != 0.0 || loc.longitude != 0.0) {
                useCase.savePoint(currentSessionId, loc.latitude, loc.longitude, 0.0, 0)
            }
        }
    }

    /**
     * 기록 중단.
     */
    fun stopRecording() {
        // 종료 지점 저장 — 유효한 GPS 좌표인 경우에만 저장합니다.
        val loc = _uiState.value.currentLocation ?: locationManager.currentMapLine.value.currentLocation
        val isStopLocationValid = loc.latitude != 0.0 || loc.longitude != 0.0
        if (isStopLocationValid) {
            useCase.savePoint(currentSessionId, loc.latitude, loc.longitude, 0.0, _uiState.value.fishingState.value)
        }

        _uiState.update { it.copy(
            isRecording = false,
            isStopPopupPresented = false,
            startLocation = null,
            endLocation = null
        )}

        // 기록 종료 시 AppManager 상태도 해제합니다.
        FDAppManager.getInstance().setRecording(false)

        locationManager.stopTracking()
        stopTimer()
    }

    /**
     * 사진 저장.
     *
     * [개념] TakePicture 계약으로 카메라가 filesDir에 직접 파일을 저장하므로,
     *        ByteArray 대신 파일명(fileName)만 받아 UseCase로 전달합니다.
     */
    fun savePhoto(fileName: String) {
        val loc = _uiState.value.currentLocation ?: return
        val geoPoint = GeoPoint(loc.latitude, loc.longitude)
        val state = _uiState.value.fishingState.value

        val path = useCase.savePhoto(currentSessionId, fileName, geoPoint, state)
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
            isLocationDisclosurePopupPresented = false,
            shouldProceedToRecording = false,
            isLocationRationalePopupPresented = false,
            isLocationPermissionPopupPresented = false,
            isCameraRationalePopupPresented = false,
            isCameraPermissionPopupPresented = false
        )}
    }

    fun showStopPopup() {
        _uiState.update { it.copy(isStopPopupPresented = true) }
    }

    // 기록 시작 버튼 클릭 시 위치 수집 목적을 안내합니다.
    // [개념] Google Play 정책상 위치 정보 수집 전 명확한 고지가 필요합니다.
    //        최초 1회만 팝업을 표시하며, 이후에는 바로 기록 시작 흐름으로 진입합니다.
    fun showLocationDisclosurePopup() {
        val hasSeen = AppPreferences.getBoolean(
            getApplication(),
            PreferenceConstants.HAS_SEEN_LOCATION_DISCLOSURE
        )
        if (hasSeen) {
            // 이미 동의한 경우: 팝업 없이 바로 진행 (Screen이 LaunchedEffect로 처리)
            _uiState.update { it.copy(shouldProceedToRecording = true) }
        } else {
            // 최초 진입: 위치 수집 안내 팝업 표시
            _uiState.update { it.copy(isLocationDisclosurePopupPresented = true) }
        }
    }

    // 사용자가 위치 수집 안내에 동의한 경우 호출합니다.
    // [개념] 동의 여부를 SharedPreferences에 저장하여 이후 실행 시 팝업을 생략합니다.
    //        iOS의 UserDefaults.standard.set(true, forKey:)에 대응합니다.
    fun confirmLocationDisclosure() {
        AppPreferences.setBoolean(
            getApplication(),
            PreferenceConstants.HAS_SEEN_LOCATION_DISCLOSURE,
            true
        )
        _uiState.update { it.copy(isLocationDisclosurePopupPresented = false, shouldProceedToRecording = true) }
    }

    // Screen이 shouldProceedToRecording 처리 완료 후 플래그를 초기화합니다.
    fun clearProceedToRecording() {
        _uiState.update { it.copy(shouldProceedToRecording = false) }
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
    val startLocation: Location? = null,
    val endLocation: Location? = null,
    val markers: List<StateChangeMarker> = emptyList(),
    val photoMarkers: List<PhotoMarker> = emptyList(),
    val savedImagePaths: List<String> = emptyList(),
    val savedPointCount: Int = 0,
    val isStopPopupPresented: Boolean = false,
    // 기록 시작 전 위치 수집 목적 안내 팝업 (Google Play 정책 대응, 최초 1회)
    val isLocationDisclosurePopupPresented: Boolean = false,
    // 동의 완료 후 Screen에서 권한 체크 → 기록 시작 흐름으로 진입하도록 신호
    // [개념] ViewModel이 직접 권한 요청을 할 수 없으므로 State 플래그로 Screen에 위임합니다.
    //        Screen의 LaunchedEffect가 이 값을 감지하여 처리한 뒤 clearProceedToRecording()으로 초기화합니다.
    val shouldProceedToRecording: Boolean = false,
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
