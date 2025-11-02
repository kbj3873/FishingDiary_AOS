package com.bj.fishingdiary.domain.entity

/**
 * 낚시 포인트 지도 관련 Entity
 * Fishing Point Map Related Entities
 *
 * 이 파일은 낚시 위치 추적 및 지도 표시를 위한 데이터 모델을 정의합니다.
 * This file defines data models for fishing location tracking and map display.
 */

/**
 * 지도 타입 열거형
 * Map Type Enumeration
 *
 * enum class란?
 * - 정해진 값들만 가질 수 있는 타입
 * - 타입 안정성을 제공 (잘못된 값 방지)
 * - iOS의 enum과 동일한 개념
 *
 * Android에서 지원하는 지도 제공자:
 * Map providers supported in Android:
 * - APPLE_MAP: Google Maps (Android에는 Apple Maps가 없으므로 대신 사용)
 * - KAKAO_MAP: Kakao Maps (한국 전용)
 */
enum class MapType {
    APPLE_MAP,    // Google Maps를 사용 (iOS Apple Maps에 대응)
    KAKAO_MAP     // Kakao Maps
}

/**
 * 위치 데이터
 * Location Data
 *
 * GPS로 추적한 낚시 위치 정보를 저장하는 모델
 * Model that stores fishing location information tracked by GPS
 *
 * @property time 기록 시간 (예: "2024-01-15 14:30:00")
 * @property latitude 위도 (Latitude) - 문자열 형식
 * @property longitude 경도 (Longitude) - 문자열 형식
 * @property kmh 속도 (km/h)
 * @property knot 속도 (knot, 해양에서 사용하는 속도 단위)
 * @property sequence 순서 번호 (여러 포인트를 기록할 때 순서 구분)
 */
data class LocationData(
    val time: String,
    val latitude: String,
    val longitude: String,
    val kmh: String,
    val knot: String,
    val sequence: Int
)

/**
 * DMS (Degrees Minutes Seconds) 표시 형식
 * DMS Display Format
 *
 * 좌표 표시 형식:
 * Coordinate display format:
 * - D: 도(Degrees)만 표시 (예: 37.5665°)
 * - DM: 도/분(Minutes) 표시 (예: 37° 33.99')
 * - DMS: 도/분/초(Seconds) 표시 (예: 37° 33' 59.4")
 *
 * sealed class란?
 * - enum의 확장 버전으로, 각 케이스가 다른 데이터를 가질 수 있음
 * - iOS Swift의 enum with associated values와 유사
 */
sealed class DMSType(val description: String) {
    /**
     * 도(Degrees)만 표시
     * Display Degrees only
     */
    object D : DMSType("D")

    /**
     * 도/분(Degrees/Minutes) 표시
     * Display Degrees and Minutes
     */
    object DM : DMSType("D/M")

    /**
     * 도/분/초(Degrees/Minutes/Seconds) 표시
     * Display Degrees, Minutes, and Seconds
     */
    object DMS : DMSType("D/M/S")

    /**
     * 다음 형식으로 전환
     * Switch to next format
     *
     * D -> DM -> DMS -> D (순환)
     * D -> DM -> DMS -> D (cycle)
     */
    fun next(): DMSType {
        return when (this) {
            is D -> DM
            is DM -> DMS
            is DMS -> D
        }
    }
}

/**
 * 지도 핀 (마커) 정보
 * Map Pin (Marker) Information
 *
 * 지도에 표시될 낚시 포인트 마커의 정보
 * Information of fishing point marker to be displayed on the map
 *
 * @property title 마커 제목 (예: "포인트 1")
 * @property subtitle 마커 부제목 (예: "2024-01-15")
 * @property latitude 위도
 * @property longitude 경도
 * @property locationData 상세 위치 데이터
 * @property dmsType 좌표 표시 형식
 */
data class MapPin(
    val title: String?,
    val subtitle: String?,
    val latitude: Double,
    val longitude: Double,
    val locationData: LocationData,
    var dmsType: DMSType = DMSType.D  // 기본값은 D (도만 표시)
)

/**
 * 지도 라인 정보 (경로 표시용)
 * Map Line Information (for path display)
 *
 * 두 지점을 연결하는 선을 그리기 위한 정보
 * Information to draw a line connecting two points
 *
 * @property previousLatitude 이전 위치 위도
 * @property previousLongitude 이전 위치 경도
 * @property currentLatitude 현재 위치 위도
 * @property currentLongitude 현재 위치 경도
 */
data class MapLineInfo(
    val previousLatitude: Double,
    val previousLongitude: Double,
    val currentLatitude: Double,
    val currentLongitude: Double
)
