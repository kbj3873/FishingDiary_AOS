package com.bj.fishingdiary.data.repository

import com.bj.fishingdiary.data.storage.FileDataStorage
import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.LocationData
import com.bj.fishingdiary.domain.repository.TrackMapRepository
import com.bj.fishingdiary.managers.FDAppManager

/**
 * TrackMapRepository 구현체
 * TrackMapRepository Implementation
 *
 * iOS의 DefaultTrackMapRepository.swift에 대응
 * FileDataStorage를 사용하여 실시간 트랙 데이터를 관리
 */
class DefaultTrackMapRepository(
    private val fileStorage: FileDataStorage
) : TrackMapRepository {

    // 현재 추적 중인 위치 데이터를 메모리에 임시 저장
    // Temporarily store currently tracking location data in memory
    private val trackingDataCache = mutableListOf<LocationData>()

    override fun saveTrackingLocation(
        locationData: LocationData,
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        try {
            // 1. trackingDataCache에 추가
            trackingDataCache.add(locationData)

            // 2. 일정 개수마다 파일에 임시 저장 (iOS의 SAVE_FOR_POINTS와 동일)
            if (trackingDataCache.size >= FDAppManager.SAVE_FOR_POINTS) {
                // 마지막 포인트를 제외하고 저장
                val saveList = trackingDataCache.dropLast(1)
                fileStorage.savePoints(saveList)

                // 마지막 포인트만 남기고 초기화
                val lastPoint = trackingDataCache.last()
                trackingDataCache.clear()
                trackingDataCache.add(lastPoint)
            }

            // 3. 결과를 completion으로 반환
            completion(Result.success(Unit))
        } catch (e: Exception) {
            completion(Result.failure(e))
        }

        return null
    }

    override fun saveTrackToFile(
        fileName: String,
        trackData: List<LocationData>,
        completion: (Result<String>) -> Unit
    ): Cancellable? {
        try {
            // 1. fileStorage.savePoints(trackData) 호출
            fileStorage.savePoints(trackData)

            // 2. 저장 성공 시 파일 경로 반환
            // TODO: 실제 저장된 파일 경로 가져오기 (현재는 임시 경로)
            val filePath = "saved/$fileName"
            completion(Result.success(filePath))
        } catch (e: Exception) {
            completion(Result.failure(e))
        }

        return null
    }

    override fun clearTrackingData(
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        // 추적 데이터 초기화
        trackingDataCache.clear()
        completion(Result.success(Unit))
        return null
    }
}
