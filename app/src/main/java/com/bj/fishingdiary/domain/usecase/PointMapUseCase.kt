package com.bj.fishingdiary.domain.usecase

import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.LocationData
import com.bj.fishingdiary.domain.repository.PointMapRepository

/**
 * 낚시 포인트 지도 UseCase
 * Fishing Point Map UseCase
 *
 * 낚시 포인트 데이터를 조회, 저장, 삭제하는 비즈니스 로직
 * Business logic for querying, saving, and deleting fishing point data
 *
 * @property pointMapRepository 포인트 지도 Repository
 */
class PointMapUseCase(
    private val pointMapRepository: PointMapRepository
) : UseCase {

    /**
     * 포인트 데이터 로드 실행
     * Execute load point data
     *
     * @param requestValue 요청 값 (파일 경로 포함)
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun executeLoad(
        requestValue: LoadPointRequestValue,
        completion: (Result<List<LocationData>>) -> Unit
    ): Cancellable? {
        return pointMapRepository.loadPointData(
            filePath = requestValue.filePath,
            completion = { result ->
                // 필요시 데이터 가공 (예: 정렬, 필터링)
                // Process data if needed (e.g., sorting, filtering)
                result.onSuccess { locationList ->
                    // 시퀀스 순서대로 정렬
                    // Sort by sequence order
                    val sortedList = locationList.sortedBy { it.sequence }
                    completion(Result.success(sortedList))
                }.onFailure { error ->
                    completion(Result.failure(error))
                }
            }
        )
    }

    /**
     * 포인트 데이터 저장 실행
     * Execute save point data
     */
    fun executeSave(
        requestValue: SavePointRequestValue,
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        return pointMapRepository.savePointData(
            filePath = requestValue.filePath,
            data = requestValue.data,
            completion = completion
        )
    }

    /**
     * 포인트 데이터 삭제 실행
     * Execute delete point data
     */
    fun executeDelete(
        requestValue: DeletePointRequestValue,
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        return pointMapRepository.deletePointData(
            filePath = requestValue.filePath,
            completion = completion
        )
    }

    /**
     * UseCase 시작 (기본 인터페이스 구현)
     * Start UseCase (base interface implementation)
     */
    override fun start(): Cancellable? {
        // 특정 작업 없이 null 반환
        // Return null without specific operation
        return null
    }
}

/**
 * 포인트 로드 요청 값
 * Load point request value
 */
data class LoadPointRequestValue(
    val filePath: String
)

/**
 * 포인트 저장 요청 값
 * Save point request value
 */
data class SavePointRequestValue(
    val filePath: String,
    val data: List<LocationData>
)

/**
 * 포인트 삭제 요청 값
 * Delete point request value
 */
data class DeletePointRequestValue(
    val filePath: String
)
