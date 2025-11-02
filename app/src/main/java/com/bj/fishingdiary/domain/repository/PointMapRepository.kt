package com.bj.fishingdiary.domain.repository

import com.bj.fishingdiary.domain.common.Cancellable
import com.bj.fishingdiary.domain.entity.LocationData

/**
 * 낚시 포인트 지도 Repository 인터페이스
 * Fishing Point Map Repository Interface
 *
 * 낚시 포인트 데이터를 로컬 파일 시스템에서 읽고 쓰는 작업을 추상화
 * Abstracts operations for reading and writing fishing point data from/to local file system
 */
interface PointMapRepository {

    /**
     * 특정 파일에서 포인트 데이터 로드
     * Load point data from a specific file
     *
     * @param filePath 파일 경로
     * @param completion 완료 콜백
     *                   - 성공: List<LocationData> 반환
     *                   - 실패: Throwable 반환
     * @return 취소 가능한 작업 객체
     */
    fun loadPointData(
        filePath: String,
        completion: (Result<List<LocationData>>) -> Unit
    ): Cancellable?

    /**
     * 포인트 데이터를 파일에 저장
     * Save point data to a file
     *
     * @param filePath 저장할 파일 경로
     * @param data 저장할 위치 데이터 목록
     * @param completion 완료 콜백
     *                   - 성공: Unit (아무것도 반환하지 않음)
     *                   - 실패: Throwable 반환
     * @return 취소 가능한 작업 객체
     */
    fun savePointData(
        filePath: String,
        data: List<LocationData>,
        completion: (Result<Unit>) -> Unit
    ): Cancellable?

    /**
     * 포인트 데이터 삭제
     * Delete point data
     *
     * @param filePath 삭제할 파일 경로
     * @param completion 완료 콜백
     * @return 취소 가능한 작업 객체
     */
    fun deletePointData(
        filePath: String,
        completion: (Result<Unit>) -> Unit
    ): Cancellable?
}
