package com.onbada.seathermo.data.repository

import com.onbada.seathermo.data.storage.FileDataStorage
import com.onbada.seathermo.domain.common.Cancellable
import com.onbada.seathermo.domain.entity.PointDate
import com.onbada.seathermo.domain.entity.PointData
import com.onbada.seathermo.domain.repository.PointDataListRepository

/**
 * PointDataListRepository 구현체
 * PointDataListRepository Implementation
 *
 * iOS의 DefaultPointDataListRepository.swift에 대응
 * FileDataStorage를 사용하여 특정 날짜 폴더 내의 데이터 파일 목록을 관리
 */
class DefaultPointDataListRepository(
    private val fileDataStorage: FileDataStorage
) : PointDataListRepository {

    override fun fetchDataList(
        pointDate: PointDate,
        completion: (Result<List<PointData>>) -> Unit
    ): Cancellable? {
        // iOS와 동일한 방식으로 FileDataStorage 호출
        fileDataStorage.fetchPointDataList(pointDate, completion)
        return null  // TODO: Cancellable 구현
    }

    override fun deleteDataFile(
        pointData: PointData,
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        // TODO: 데이터 파일 삭제
        // 1. FileManager를 통해 파일 삭제
        // 2. 결과를 completion으로 반환
        TODO("FileManager 구현 후 작성")
    }
}
