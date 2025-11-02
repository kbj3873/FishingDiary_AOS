package com.bj.fishingdiary.domain.repository

import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.LocationData

/**
 * 트랙 지도 Repository 인터페이스
 * Track Map Repository Interface
 *
 * GPS 기반 실시간 위치 추적 데이터를 관리
 * Manages GPS-based real-time location tracking data
 *
 * TrackMap vs PointMap의 차이:
 * Difference between TrackMap and PointMap:
 * - TrackMap: 실시간 이동 경로 추적 (연속적인 라인)
 * - PointMap: 저장된 특정 포인트 표시 (개별 마커)
 *
 * - TrackMap: Real-time movement path tracking (continuous line)
 * - PointMap: Display saved specific points (individual markers)
 */
interface TrackMapRepository {

    /**
     * 현재 추적 중인 위치 데이터 저장
     * Save currently tracking location data
     *
     * @param locationData 위치 데이터
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun saveTrackingLocation(
        locationData: LocationData,
        completion: (Result<Unit>) -> Unit
    ): Cancellable?

    /**
     * 추적 데이터를 파일로 저장 (추적 종료 시)
     * Save tracking data to file (when tracking ends)
     *
     * @param fileName 저장할 파일 이름
     * @param trackData 추적한 위치 데이터 목록
     * @param completion 완료 콜백
     *                   - 성공: 저장된 파일 경로 반환
     *                   - 실패: Throwable 반환
     * @return 취소 가능한 작업 객체
     */
    fun saveTrackToFile(
        fileName: String,
        trackData: List<LocationData>,
        completion: (Result<String>) -> Unit
    ): Cancellable?

    /**
     * 현재 추적 데이터 초기화 (새로운 추적 시작 시)
     * Clear current tracking data (when starting new tracking)
     *
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun clearTrackingData(
        completion: (Result<Unit>) -> Unit
    ): Cancellable?
}
