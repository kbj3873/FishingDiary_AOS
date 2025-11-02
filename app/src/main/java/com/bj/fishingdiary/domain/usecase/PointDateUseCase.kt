package com.bj.fishingdiary.domain.usecase

import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.PointDate
import com.bj.fishingdiary.domain.entity.PointData
import com.bj.fishingdiary.domain.repository.PointDateListRepository
import com.bj.fishingdiary.domain.repository.PointDataListRepository

/**
 * 낚시 포인트 날짜 목록 UseCase
 * Fishing Point Date List UseCase
 *
 * 날짜별 낚시 기록 폴더를 관리하는 비즈니스 로직
 * Business logic for managing fishing record folders by date
 *
 * @property pointDateListRepository 날짜 목록 Repository
 */
class PointDateUseCase(
    private val pointDateListRepository: PointDateListRepository
) : UseCase {

    /**
     * 날짜 목록 조회 실행
     * Execute fetch date list
     */
    fun executeFetchDateList(
        completion: (Result<List<PointDate>>) -> Unit
    ): Cancellable? {
        return pointDateListRepository.fetchDateList { result ->
            result.onSuccess { dateList ->
                // 날짜 내림차순 정렬 (최신 날짜가 먼저)
                // Sort by date descending (newest first)
                val sortedList = dateList.sortedByDescending { it.date }
                completion(Result.success(sortedList))
            }.onFailure { error ->
                completion(Result.failure(error))
            }
        }
    }

    /**
     * 날짜 폴더 생성 실행
     * Execute create date folder
     */
    fun executeCreateDateFolder(
        requestValue: CreateDateFolderRequestValue,
        completion: (Result<PointDate>) -> Unit
    ): Cancellable? {
        return pointDateListRepository.createDateFolder(
            date = requestValue.date,
            completion = completion
        )
    }

    /**
     * 날짜 폴더 삭제 실행
     * Execute delete date folder
     */
    fun executeDeleteDateFolder(
        requestValue: DeleteDateFolderRequestValue,
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        return pointDateListRepository.deleteDateFolder(
            pointDate = requestValue.pointDate,
            completion = completion
        )
    }

    override fun start(): Cancellable? = null
}

/**
 * 낚시 포인트 데이터 목록 UseCase
 * Fishing Point Data List UseCase
 *
 * 특정 날짜 폴더 내의 개별 낚시 기록 파일을 관리하는 비즈니스 로직
 * Business logic for managing individual fishing record files within a specific date folder
 *
 * @property pointDataListRepository 데이터 목록 Repository
 */
class PointDataUseCase(
    private val pointDataListRepository: PointDataListRepository
) : UseCase {

    /**
     * 데이터 목록 조회 실행
     * Execute fetch data list
     */
    fun executeFetchDataList(
        requestValue: FetchDataListRequestValue,
        completion: (Result<List<PointData>>) -> Unit
    ): Cancellable? {
        return pointDataListRepository.fetchDataList(
            pointDate = requestValue.pointDate,
            completion = { result ->
                result.onSuccess { dataList ->
                    // 파일 이름 기준 정렬
                    // Sort by file name
                    val sortedList = dataList.sortedBy { it.dataName }
                    completion(Result.success(sortedList))
                }.onFailure { error ->
                    completion(Result.failure(error))
                }
            }
        )
    }

    /**
     * 데이터 파일 삭제 실행
     * Execute delete data file
     */
    fun executeDeleteDataFile(
        requestValue: DeleteDataFileRequestValue,
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        return pointDataListRepository.deleteDataFile(
            pointData = requestValue.pointData,
            completion = completion
        )
    }

    override fun start(): Cancellable? = null
}

// ===== Request Value 객체들 =====
// Request Value Objects

/**
 * 날짜 폴더 생성 요청 값
 */
data class CreateDateFolderRequestValue(
    val date: String
)

/**
 * 날짜 폴더 삭제 요청 값
 */
data class DeleteDateFolderRequestValue(
    val pointDate: PointDate
)

/**
 * 데이터 목록 조회 요청 값
 */
data class FetchDataListRequestValue(
    val pointDate: PointDate
)

/**
 * 데이터 파일 삭제 요청 값
 */
data class DeleteDataFileRequestValue(
    val pointData: PointData
)
