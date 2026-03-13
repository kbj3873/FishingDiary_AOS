package com.bj.fishingdiary.presentation.track

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bj.fishingdiary.domain.entity.LocationData
import com.bj.fishingdiary.domain.usecase.SaveToFileRequestValue
import com.bj.fishingdiary.domain.usecase.SaveTrackingRequestValue
import com.bj.fishingdiary.domain.usecase.TrackMapUseCase
import com.bj.fishingdiary.managers.FDLocationManager
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 트랙 지도 ViewModel
 * Track Map ViewModel
 *
 * iOS의 TrackMapViewModel에 대응
 * 실시간 GPS 추적 및 경로 표시를 담당
 */
class TrackMapViewModel(
    private val context: android.content.Context,
    private val trackMapUseCase: TrackMapUseCase
) : ViewModel() {

    // 현재 속도 (km/h)
    private val _currentSpeed = MutableStateFlow("0.00")
    val currentSpeed: StateFlow<String> = _currentSpeed.asStateFlow()

    // 현재 위도
    private val _currentLatitude = MutableStateFlow("0.000000")
    val currentLatitude: StateFlow<String> = _currentLatitude.asStateFlow()

    // 현재 경도
    private val _currentLongitude = MutableStateFlow("0.000000")
    val currentLongitude: StateFlow<String> = _currentLongitude.asStateFlow()

    // 추적 상태
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // 실시간 경로 라인 (이전 위치 -> 현재 위치)
    // Pair<이전 위치, 현재 위치>
    private val _trackLineSegment = MutableStateFlow<Pair<LatLng, LatLng>?>(null)
    val trackLineSegment: StateFlow<Pair<LatLng, LatLng>?> = _trackLineSegment.asStateFlow()

    // 현재 위치
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    // 추적한 모든 위치 데이터 (파일 저장용)
    private val trackingData = mutableListOf<LocationData>()

    // 이전 위치 (경로 라인 그리기용)
    private var previousLocation: Location? = null

    // 위치 업데이트 콜백
    private val locationCallback: (Location) -> Unit = { location ->
        onLocationUpdate(location)
    }

    init {
        // FDLocationManager의 위치 업데이트 콜백 등록
        FDLocationManager.getInstance(context).setLocationUpdateCallback(locationCallback)
    }

    /**
     * 추적 시작
     */
    fun startTracking() {
        android.util.Log.d("Location Debug", "TrackMapViewModel.startTracking called")
        if (_isTracking.value) return

        viewModelScope.launch {
            // 1. 디렉토리 생성 (iOS와 동일)
            val dateResult = trackMapUseCase.executeCreatePointDate()
            val dataResult = trackMapUseCase.executeCreatePointData()

            dateResult.onSuccess {
                android.util.Log.d("TrackMapViewModel", "Point date directory created")
            }.onFailure { error ->
                android.util.Log.w("TrackMapViewModel", "Point date directory already exists or failed: $error")
            }

            dataResult.onSuccess {
                android.util.Log.d("TrackMapViewModel", "Point data directory created")
            }.onFailure { error ->
                android.util.Log.w("TrackMapViewModel", "Point data directory failed: $error")
            }

            // 2. 추적 데이터 초기화
            trackMapUseCase.executeClearTracking { result ->
                result.onSuccess {
                    android.util.Log.d("TrackMapViewModel", "Tracking data cleared")
                }
            }

            // 3. 내부 데이터 초기화
            trackingData.clear()
            previousLocation = null

            // 4. FDLocationManager 시작
            FDLocationManager.getInstance(context).startTracking()

            // 5. 추적 상태 업데이트
            _isTracking.value = true

            android.util.Log.d("TrackMapViewModel", "Tracking started")
        }
    }

    /**
     * 추적 중지 및 저장
     */
    fun stopTracking() {
        if (!_isTracking.value) return

        viewModelScope.launch {
            // 1. FDLocationManager 중지
            FDLocationManager.getInstance(context).stopTracking()

            // 2. 추적 상태 업데이트
            _isTracking.value = false

            // 3. 파일로 저장 (Point와 호환되는 형식)
            saveTrackingData()

            android.util.Log.d("TrackMapViewModel", "Tracking stopped")
        }
    }

    /**
     * 위치 업데이트 처리
     */
    private fun onLocationUpdate(location: Location) {
        android.util.Log.d("Location Debug", "ViewModel received location update: ${location.latitude}, ${location.longitude}")
        // 1. UI 업데이트
        updateUI(location)

        // 2. LocationData 생성
        val locationData = createLocationData(location)

        // 3. 추적 데이터에 추가
        trackingData.add(locationData)

        // 4. UseCase를 통해 저장 (자동 저장)
        val saveRequest = SaveTrackingRequestValue(locationData)
        trackMapUseCase.executeSaveTracking(saveRequest) { result ->
            result.onSuccess {
                android.util.Log.d("TrackMapViewModel", "Location saved: ${locationData.sequence}")
            }.onFailure { error ->
                android.util.Log.e("TrackMapViewModel", "Failed to save location", error)
            }
        }

        // 5. 경로 라인 업데이트 (이전 위치 -> 현재 위치)
        previousLocation?.let { prev ->
            val prevLatLng = LatLng(prev.latitude, prev.longitude)
            val currLatLng = LatLng(location.latitude, location.longitude)
            _trackLineSegment.value = Pair(prevLatLng, currLatLng)
        }

        // 6. 현재 위치 업데이트
        _currentLocation.value = LatLng(location.latitude, location.longitude)

        // 7. 이전 위치 갱신
        previousLocation = location
    }

    /**
     * UI 업데이트
     */
    private fun updateUI(location: Location) {
        _currentLatitude.value = String.format("%.6f", location.latitude)
        _currentLongitude.value = String.format("%.6f", location.longitude)

        // 속도 (m/s -> km/h 변환)
        val speed = location.speed * 3.6
        _currentSpeed.value = String.format("%.2f", speed)
    }

    /**
     * LocationData 생성
     */
    private fun createLocationData(location: Location): LocationData {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val time = sdf.format(Date(location.time))

        val kmh = location.speed * 3.6 // m/s -> km/h
        val knot = location.speed * 1.94384 // m/s -> knot

        return LocationData(
            time = time,
            latitude = String.format("%.6f", location.latitude),
            longitude = String.format("%.6f", location.longitude),
            kmh = String.format("%.2f", kmh),
            knot = String.format("%.2f", knot),
            sequence = trackingData.size + 1
        )
    }

    /**
     * 추적 데이터를 파일로 저장 (Point와 호환)
     */
    private fun saveTrackingData() {
        if (trackingData.isEmpty()) {
            android.util.Log.w("TrackMapViewModel", "No tracking data to save")
            return
        }

        // 파일 이름 생성 (날짜_시간 형식)
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "track_${sdf.format(Date())}.json"

        val saveRequest = SaveToFileRequestValue(
            fileName = fileName,
            trackData = trackingData.toList()
        )

        trackMapUseCase.executeSaveToFile(saveRequest) { result ->
            result.onSuccess { filePath ->
                android.util.Log.d("TrackMapViewModel", "Track saved to: $filePath")
            }.onFailure { error ->
                android.util.Log.e("TrackMapViewModel", "Failed to save track", error)
            }
        }
    }

    /**
     * 현재 추적 데이터 개수
     */
    fun getTrackingDataCount(): Int = trackingData.size

    /**
     * ViewModel 정리
     */
    override fun onCleared() {
        super.onCleared()
        // 추적 중이면 중지
        if (_isTracking.value) {
            stopTracking()
        }
        // 콜백 해제
        FDLocationManager.getInstance(context).setLocationUpdateCallback(null)
    }
}
