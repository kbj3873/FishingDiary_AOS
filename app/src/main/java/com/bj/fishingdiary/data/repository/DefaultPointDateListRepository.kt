package com.bj.fishingdiary.data.repository

import com.bj.fishingdiary.data.storage.FileDataStorage
import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.PointDate
import com.bj.fishingdiary.domain.repository.PointDateListRepository
import java.io.File

/**
 * PointDateListRepository 구현체
 * PointDateListRepository Implementation
 *
 * iOS의 DefaultPointDateListRepository.swift에 대응
 * FileDataStorage를 사용하여 날짜 폴더 목록을 관리
 */
class DefaultPointDateListRepository(
    private val fileDataStorage: FileDataStorage
) : PointDateListRepository {

    override fun fetchDateList(
        completion: (Result<List<PointDate>>) -> Unit
    ): Cancellable? {
        // iOS와 동일한 방식으로 FileDataStorage 호출
        fileDataStorage.fetchPointDateList(completion)
        return null  // TODO: Cancellable 구현
    }

    override fun createDateFolder(
        date: String,
        completion: (Result<PointDate>) -> Unit
    ): Cancellable? {
        // TODO: 날짜 폴더 생성
        // 1. fileDataStorage.createPointDate() 호출
        // 2. 성공 시 PointDate 객체 생성하여 반환
        TODO("FileManager 구현 후 작성")
    }

    override fun deleteDateFolder(
        pointDate: PointDate,
        completion: (Result<Unit>) -> Unit
    ): Cancellable? {
        // TODO: 날짜 폴더 삭제
        // 1. FileManager를 통해 폴더 삭제
        // 2. 결과를 completion으로 반환
        TODO("FileManager 구현 후 작성")
    }
}
