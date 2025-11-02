package com.bj.fishingdiary.domain.usecase

import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.LocationData
import com.bj.fishingdiary.domain.repository.TrackMapRepository

/**
 * 트랙 지도 UseCase
 * Track Map UseCase
 *
 * GPS 기반 실시간 위치 추적을 관리하는 비즈니스 로직
 * Business logic for managing GPS-based real-time location tracking
 *
 * 추적 흐름:
 * Tracking flow:
 * 1. clearTrackingData() - 새로운 추적 시작 전 초기화
 * 2. saveTrackingLocation() - GPS 위치 업데이트마다 호출
 * 3. saveTrackToFile() - 추적 종료 시 파일로 저장
 *
 * @property trackMapRepository 트랙 지도 Repository
 */
class TrackMapUseCase(
    private val trackMapRepository: TrackMapRepository
) : UseCase {

    /**
     * 추적 위치 저장 실행
     * Execute save tracking location
     *
     * GPS가 새로운 위치를 업데이트할 때마다 호출됩니다
     * Called whenever GPS updates a new location
     *
     * @param requestValue 요청 값 (위치 데이터 포함)
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun executeSaveTracking(
        requestValue: SaveTrackingRequestValue,
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        // 위치 데이터 유효성 검사
        // Validate location data
        val locationData = requestValue.locationData

        // 위도/경도가 유효한 범위인지 확인
        // Check if latitude/longitude are in valid range
        val isValidLocation = try {
            val lat = locationData.latitude.toDouble()
            val lon = locationData.longitude.toDouble()
            lat in -90.0..90.0 && lon in -180.0..180.0
        } catch (e: NumberFormatException) {
            false
        }

        return if (isValidLocation) {
            trackMapRepository.saveTrackingLocation(
                locationData = locationData,
                completion = completion
            )
        } else {
            // 유효하지 않은 위치 데이터인 경우 에러 반환
            // Return error for invalid location data
            completion(Result.failure(IllegalArgumentException("Invalid location data")))
            null
        }
    }

    /**
     * 추적 데이터를 파일로 저장 실행
     * Execute save track to file
     *
     * 추적을 종료하고 데이터를 영구 저장할 때 호출됩니다
     * Called when ending tracking and permanently saving data
     *
     * @param requestValue 요청 값 (파일 이름 및 추적 데이터 포함)
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun executeSaveToFile(
        requestValue: SaveToFileRequestValue,
        completion: (Result<String>) -> Unit
    ): Cancellable? {
        // 파일 이름 유효성 검사
        // Validate file name
        val fileName = requestValue.fileName
        if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")) {
            completion(Result.failure(IllegalArgumentException("Invalid file name")))
            return null
        }

        // 추적 데이터가 비어있는지 확인
        // Check if tracking data is empty
        if (requestValue.trackData.isEmpty()) {
            completion(Result.failure(IllegalStateException("No tracking data to save")))
            return null
        }

        return trackMapRepository.saveTrackToFile(
            fileName = fileName,
            trackData = requestValue.trackData,
            completion = completion
        )
    }

    /**
     * 추적 데이터 초기화 실행
     * Execute clear tracking data
     *
     * 새로운 추적을 시작하기 전에 호출됩니다
     * Called before starting a new tracking session
     */
    fun executeClearTracking(
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        return trackMapRepository.clearTrackingData(
            completion = completion
        )
    }

    override fun start(): Cancellable? = null
}

// ===== Request Value 객체들 =====
// Request Value Objects

/**
 * 추적 위치 저장 요청 값
 * Save tracking location request value
 */
data class SaveTrackingRequestValue(
    val locationData: LocationData
)

/**
 * 파일 저장 요청 값
 * Save to file request value
 */
data class SaveToFileRequestValue(
    val fileName: String,
    val trackData: List<LocationData>
)

/**
 * TrackMapUseCase 사용 예시:
 * TrackMapUseCase usage example:
 *
 * ```kotlin
 * // 1. UseCase 생성
 * // Create UseCase
 * val trackMapUseCase = TrackMapUseCase(trackMapRepository)
 *
 * // 2. 추적 시작 - 데이터 초기화
 * // Start tracking - Clear data
 * trackMapUseCase.executeClearTracking { result ->
 *     result.onSuccess {
 *         println("Tracking cleared, ready to start")
 *     }
 * }
 *
 * // 3. 위치 업데이트 (GPS 콜백에서 호출)
 * // Location update (called from GPS callback)
 * val locationData = LocationData(
 *     time = "2024-01-15 14:30:00",
 *     latitude = "37.5665",
 *     longitude = "126.9780",
 *     kmh = "5.2",
 *     knot = "2.8",
 *     sequence = 1
 * )
 * val saveRequest = SaveTrackingRequestValue(locationData)
 * trackMapUseCase.executeSaveTracking(saveRequest) { result ->
 *     result.onSuccess {
 *         println("Location saved")
 *     }
 * }
 *
 * // 4. 추적 종료 - 파일로 저장
 * // End tracking - Save to file
 * val saveFileRequest = SaveToFileRequestValue(
 *     fileName = "morning_fishing_2024-01-15.json",
 *     trackData = listOf(locationData, ...) // 모든 추적 데이터
 * )
 * trackMapUseCase.executeSaveToFile(saveFileRequest) { result ->
 *     result.onSuccess { filePath ->
 *         println("Saved to: $filePath")
 *     }
 * }
 * ```
 */
