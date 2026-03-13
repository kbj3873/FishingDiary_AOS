package com.onbada.seathermo.presentation.point

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.domain.entity.DMSType
import com.onbada.seathermo.domain.entity.LocationData
import com.onbada.seathermo.domain.entity.MapPin
import com.onbada.seathermo.domain.entity.PointData
import com.onbada.seathermo.domain.usecase.PointMapUseCase
import com.onbada.seathermo.managers.FDAppManager
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 포인트 지도 ViewModel
 * Point Map ViewModel
 *
 * iOS의 PointMapViewModel 대응
 */
class PointMapViewModel(
    private val pointData: PointData,
    private val pointMapUseCase: PointMapUseCase
) : ViewModel() {

    // 지도 영역 (Camera Position)
    private val _cameraBounds = MutableStateFlow<LatLngBounds?>(null)
    val cameraBounds: StateFlow<LatLngBounds?> = _cameraBounds.asStateFlow()

    // 폴리라인 (이동 경로) - 색상별로 분리하여 관리
    // Pair<List<LatLng>, Boolean>: 좌표 리스트와 isSlow(낚시중) 여부
    private val _polyLineSegments = MutableStateFlow<List<Pair<List<LatLng>, Boolean>>>(emptyList())
    val polyLineSegments: StateFlow<List<Pair<List<LatLng>, Boolean>>> = _polyLineSegments.asStateFlow()

    // 마커 (포인트 핀)
    private val _mapPins = MutableStateFlow<List<MapPin>>(emptyList())
    val mapPins: StateFlow<List<MapPin>> = _mapPins.asStateFlow()
    
    // 로딩 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadLocations()
    }

    private fun loadLocations() {
        if (_isLoading.value) return
        _isLoading.value = true

        // UseCase ExecuteLoad 호출
        val requestValue = com.onbada.seathermo.domain.usecase.LoadPointRequestValue(pointData.dataPath?.absolutePath ?: "")
        
        pointMapUseCase.executeLoad(requestValue) { result ->
            result.onSuccess { locations ->
                processLocations(locations)
            }.onFailure { error ->
                // 에러 처리
                error.printStackTrace()
            }
            _isLoading.value = false
        }
    }

    private fun processLocations(locations: List<LocationData>) {
        if (locations.isEmpty()) return

        val latLngs = locations.mapNotNull { loc ->
            try {
                LatLng(loc.latitude.toDouble(), loc.longitude.toDouble())
            } catch (e: Exception) {
                null
            }
        }

        if (latLngs.isEmpty()) return

        // 1. 카메라 영역 설정 (Region)
        updateCameraBounds(latLngs)

        // 2. 폴리라인 및 마커 생성 로직 (iOS 로직 이식)
        generateMapItems(locations)
    }

    private fun updateCameraBounds(latLngs: List<LatLng>) {
        val builder = LatLngBounds.Builder()
        latLngs.forEach { builder.include(it) }
        _cameraBounds.value = builder.build()
    }

    private fun generateMapItems(locations: List<LocationData>) {
        val segments = mutableListOf<Pair<List<LatLng>, Boolean>>()
        val pins = mutableListOf<MapPin>()

        if (locations.isEmpty()) return

        // 색상이 같은 연속된 포인트들을 하나의 세그먼트로 합치기
        var currentSegment = mutableListOf<LatLng>()
        var currentIsSlow: Boolean? = null

        for (i in 0 until locations.size) {
            val location = locations[i]
            val lat = location.latitude.toDouble()
            val lon = location.longitude.toDouble()
            val currentLatLng = LatLng(lat, lon)

            val kmh = location.kmh.toFloatOrNull() ?: 0f
            val isSlow = kmh < 5.0f

            // 색상이 바뀌면 현재 세그먼트를 저장하고 새로운 세그먼트 시작
            if (currentIsSlow != null && currentIsSlow != isSlow && currentSegment.size > 1) {
                segments.add(Pair(currentSegment.toList(), currentIsSlow))
                // 연결을 끊기지 않게 마지막 점을 새 세그먼트의 시작점으로
                currentSegment = mutableListOf(currentSegment.last(), currentLatLng)
            } else {
                currentSegment.add(currentLatLng)
            }

            currentIsSlow = isSlow

            // 마커 생성 로직 (iOS: index > 0 체크)
            if (i > 0) {
                val prevLocation = locations[i - 1]
                val prevKnot = prevLocation.knot.toDoubleOrNull() ?: 0.0
                val currentKnot = location.knot.toDoubleOrNull() ?: 0.0

                // iOS Condition: if knot < 2 && preKnot >= 2
                // 속도가 2노트 이상이었다가 2노트 미만으로 떨어지는 순간 (정지/낚시 시작)
                if (currentKnot < 2.0 && prevKnot >= 2.0) {
                    val mapPin = MapPin(
                        title = "${location.kmh} km/h",
                        subtitle = "${location.knot} knot",
                        latitude = lat,
                        longitude = lon,
                        locationData = location,
                        dmsType = DMSType.D
                    )
                    pins.add(mapPin)
                }
            }
        }

        // 마지막 세그먼트 추가
        if (currentSegment.size > 1 && currentIsSlow != null) {
            segments.add(Pair(currentSegment.toList(), currentIsSlow))
        }

        android.util.Log.d("PointMapViewModel", "Total segments created: ${segments.size}")
        segments.forEachIndexed { index, (points, isSlow) ->
            android.util.Log.d("PointMapViewModel", "Segment $index: ${points.size} points, isSlow=$isSlow")
        }

        _polyLineSegments.value = segments
        _mapPins.value = pins
    }
}

/**
 * ViewModel Factory
 */
class PointMapViewModelFactory(
    private val pointData: PointData,
    private val pointMapUseCase: PointMapUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PointMapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PointMapViewModel(pointData, pointMapUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
