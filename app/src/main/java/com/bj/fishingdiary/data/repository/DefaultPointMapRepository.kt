package com.bj.fishingdiary.data.repository

import com.bj.fishingdiary.data.storage.FileDataStorage
import com.bj.fishingdiary.data.storage.FileStorageError
import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.LocationData
import com.bj.fishingdiary.domain.entity.PointData
import com.bj.fishingdiary.domain.repository.PointMapRepository
import java.io.File

/**
 * PointMapRepository 구현체
 * PointMapRepository Implementation
 *
 * iOS의 DefaultPointMapRepository.swift에 대응
 * FileDataStorage를 사용하여 포인트 데이터를 파일 시스템에 저장/로드
 */
class DefaultPointMapRepository(
    private val fileStorage: FileDataStorage
) : PointMapRepository {

    override fun loadPointData(
        filePath: String,
        completion: (Result<List<LocationData>>) -> Unit
    ): Cancellable? {
        // 1. 파일 경로에서 PointData 생성
        val file = File(filePath)
        val pointData = PointData(
            dataName = file.name,
            dataPath = file
        )

        // 2. fileStorage.fetchLocations() 호출
        fileStorage.fetchLocations(pointData) { result ->
            // 3. 결과를 completion으로 반환
            completion(result)
        }

        return null  // FileStorage는 동기 작업이므로 취소 불가
    }

    override fun savePointData(
        filePath: String,
        data: List<LocationData>,
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        // FileDataStorage.savePoints() 호출
        try {
            fileStorage.savePoints(data)
            completion(Result.success(Unit))
        } catch (e: Exception) {
            completion(Result.failure(e))
        }

        return null
    }

    override fun deletePointData(
        filePath: String,
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        // 파일 삭제
        try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.deleteRecursively()
                if (deleted) {
                    completion(Result.success(Unit))
                } else {
                    completion(Result.failure(FileStorageError.DeleteError))
                }
            } else {
                completion(Result.failure(FileStorageError.ReadError))
            }
        } catch (e: Exception) {
            completion(Result.failure(FileStorageError.DeleteError))
        }

        return null
    }
}
