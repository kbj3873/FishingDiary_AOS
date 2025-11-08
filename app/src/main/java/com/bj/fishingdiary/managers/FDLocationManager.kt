package com.bj.fishingdiary.managers

import android.content.Context
import android.location.Location
import com.bj.fishingdiary.domain.entity.LocationData
import com.bj.fishingdiary.domain.entity.LocationInfo
import com.bj.fishingdiary.domain.entity.MapLineInfo
import com.bj.fishingdiary.services.LocationTrackingService
import com.bj.fishingdiary.utility.LocationPermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 위치 관리자
 * Location Manager
 *
 * iOS의 FDLocationManager.swift에 대응
 * 위치 추적, 데이터 수집, 저장 관리를 담당
 */
class FDLocationManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: FDLocationManager? = null

        fun getInstance(context: Context): FDLocationManager {
            return instance ?: synchronized(this) {
                instance ?: FDLocationManager(context.applicationContext).also { instance = it }
            }
        }

        // ISO 8601 날짜 포맷
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    }

    // ==================== 변수 ====================

    /**
     * 시퀀스 번호
     * Sequence number
     */
    private var sequenceNum: Int = 0

    /**
     * 현재 위도
     * Current latitude
     */
    var latitude: String = ""
        private set

    /**
     * 현재 경도
     * Current longitude
     */
    var longitude: String = ""
        private set

    /**
     * 현재 지도 라인 정보 (StateFlow)
     * Current map line information
     *
     * iOS의 CurrentValueSubject<MapLineInfo, Never>에 대응
     * 이전 위치 → 현재 위치 라인 정보를 실시간으로 전달
     */
    private val _currentMapLine = MutableStateFlow(
        MapLineInfo(
            previousLocation = createDummyLocation(0.0, 0.0),
            currentLocation = createDummyLocation(0.0, 0.0)
        )
    )
    val currentMapLine: StateFlow<MapLineInfo> = _currentMapLine.asStateFlow()

    /**
     * 위치 정보 리스트
     * Location information list
     */
    private val locationList = mutableListOf<LocationInfo>()

    /**
     * 저장 완료 콜백
     * Save completion callback
     *
     * iOS의 saveComplete: (([LocationData]) -> Void)?에 대응
     */
    var saveComplete: ((List<LocationData>) -> Unit)? = null

    /**
     * 추적 중 여부
     * Is tracking
     */
    private var isTracking: Boolean = false

    // ==================== 메서드 ====================

    /**
     * 위치 초기화
     * Initialize location
     *
     * iOS의 initLocation()에 대응
     */
    private fun initLocation() {
        latitude = ""
        longitude = ""
    }

    /**
     * 추적 시작
     * Start tracking
     *
     * iOS의 startTracking()에 대응
     */
    fun startTracking(onPermissionDenied: (() -> Unit)? = null) {
        // 이미 추적 중이면 무시
        if (isTracking) {
            return
        }

        // 초기화
        sequenceNum = 0
        initLocation()

        // 권한 체크
        if (!LocationPermissionHelper.hasAllPermissions(context, includeBackground = true)) {
            onPermissionDenied?.invoke()
            return
        }

        // 추적 시작
        isTracking = true

        // Foreground Service 시작
        LocationTrackingService.startService(context)
    }

    /**
     * 추적 중지
     * Stop tracking
     *
     * iOS의 stopTracking()에 대응
     */
    fun stopTracking() {
        // 추적 중이 아니면 무시
        if (!isTracking) {
            return
        }

        // Foreground Service 중지
        LocationTrackingService.stopService(context)

        // 추적 중지
        isTracking = false

        // 시퀀스 번호 초기화
        sequenceNum = 0

        // 남은 위치 데이터 저장
        if (locationList.isNotEmpty()) {
            val locations = locationList.map { it.locationData }
            saveComplete?.invoke(locations)
            locationList.clear()
        }
    }

    /**
     * 위치 추가
     * Add location
     *
     * iOS의 addLocation(location: CLLocation)에 대응
     * LocationTrackingService에서 호출됨
     */
    fun addLocation(location: Location) {
        // 추적 중이 아니면 무시
        if (!isTracking) {
            return
        }

        // 위도/경도 (소수점 6자리)
        val lat = String.format("%.6f", location.latitude)
        val lon = String.format("%.6f", location.longitude)

        // 속도 계산
        val speedMps = if (location.hasSpeed()) location.speed else 0f
        val kmh = String.format("%.2f", speedMps * 3.6f)  // m/s -> km/h
        val knot = String.format("%.2f", speedMps * 1.944f)  // m/s -> knot

        // 시퀀스 번호 증가
        sequenceNum++

        // LocationData 생성
        val locationData = LocationData(
            time = dateFormat.format(Date()),
            latitude = lat,
            longitude = lon,
            kmh = kmh,
            knot = knot,
            sequence = sequenceNum
        )

        // LocationInfo 생성
        val locationInfo = LocationInfo(
            locationData = locationData,
            locationInfo = location
        )

        // 리스트에 추가
        locationList.add(locationInfo)

        // 현재 위도/경도 업데이트
        latitude = lat
        longitude = lon

        // 로그 출력
        println("location data : $locationData")

        // MapLineInfo 업데이트 (StateFlow)
        if (locationList.size >= 2) {
            val previousLocation = locationList[locationList.size - 2].locationInfo
            val currentLocation = location
            val mapLine = MapLineInfo(previousLocation, currentLocation)
            _currentMapLine.value = mapLine
        }

        // 200개마다 저장 (마지막 1개는 남겨둠)
        val count = locationList.size
        if (count >= FDAppManager.SAVE_FOR_POINTS) {
            // 마지막 정보만 남겨두고 앞의 개수만큼 저장
            val saveList = locationList.dropLast(1)
            val locations = saveList.map { it.locationData }

            // 저장 콜백 호출
            saveComplete?.invoke(locations)

            // 마지막 위치만 남김
            val lastLocation = locationList.last()
            locationList.clear()
            locationList.add(lastLocation)
        }
    }

    /**
     * 더미 Location 객체 생성
     * Create dummy location
     */
    private fun createDummyLocation(latitude: Double, longitude: Double): Location {
        return Location("dummy").apply {
            this.latitude = latitude
            this.longitude = longitude
        }
    }

    /**
     * 추적 중 여부 반환
     * Is tracking
     */
    fun isTracking(): Boolean = isTracking
}
