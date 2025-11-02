package com.bj.fishingdiary.domain.repository

import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.PointDate
import com.bj.fishingdiary.domain.entity.PointData

/**
 * 낚시 포인트 날짜 목록 Repository 인터페이스
 * Fishing Point Date List Repository Interface
 *
 * 로컬 파일 시스템에 저장된 날짜별 낚시 기록 폴더 목록을 관리
 * Manages list of fishing record folders stored by date in local file system
 */
interface PointDateListRepository {

    /**
     * 날짜 폴더 목록 조회
     * Fetch date folder list
     *
     * 파일 시스템에서 날짜별로 생성된 폴더 목록을 가져옵니다
     * Retrieves list of folders created by date from file system
     *
     * @param completion 완료 콜백
     *                   - 성공: List<PointDate> 반환 (날짜 내림차순 정렬)
     *                   - 실패: Throwable 반환
     * @return 취소 가능한 작업 객체
     */
    fun fetchDateList(
        completion: (Result<List<PointDate>>) -> Unit
    ): Cancellable?

    /**
     * 날짜 폴더 생성
     * Create date folder
     *
     * @param date 날짜 문자열 (yyyy-MM-dd 형식)
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun createDateFolder(
        date: String,
        completion: (Result<PointDate>) -> Unit
    ): Cancellable?

    /**
     * 날짜 폴더 삭제
     * Delete date folder
     *
     * @param pointDate 삭제할 날짜 정보
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun deleteDateFolder(
        pointDate: PointDate,
        completion: (Result<Unit>) -> Unit
    ): Cancellable?
}

/**
 * 낚시 포인트 데이터 목록 Repository 인터페이스
 * Fishing Point Data List Repository Interface
 *
 * 특정 날짜 폴더 내의 개별 낚시 기록 파일 목록을 관리
 * Manages list of individual fishing record files within a specific date folder
 */
interface PointDataListRepository {

    /**
     * 특정 날짜의 데이터 파일 목록 조회
     * Fetch data file list for a specific date
     *
     * @param pointDate 날짜 정보
     * @param completion 완료 콜백
     *                   - 성공: List<PointData> 반환
     *                   - 실패: Throwable 반환
     * @return 취소 가능한 작업 객체
     */
    fun fetchDataList(
        pointDate: PointDate,
        completion: (Result<List<PointData>>) -> Unit
    ): Cancellable?

    /**
     * 데이터 파일 삭제
     * Delete data file
     *
     * @param pointData 삭제할 데이터 정보
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun deleteDataFile(
        pointData: PointData,
        completion: (Result<Unit>) -> Unit
    ): Cancellable?
}
