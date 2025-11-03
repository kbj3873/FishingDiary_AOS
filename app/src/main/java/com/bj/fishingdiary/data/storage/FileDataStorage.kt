package com.bj.fishingdiary.data.storage

import com.bj.fishingdiary.domain.entity.LocationData
import com.bj.fishingdiary.domain.entity.PointDate
import com.bj.fishingdiary.domain.entity.PointData

/**
 * 파일 데이터 저장소 인터페이스
 * File Data Storage Interface
 *
 * iOS의 FileDataStorageProtocol에 대응
 * 로컬 파일 시스템에 낚시 포인트 데이터를 저장/조회하는 작업 정의
 */
interface FileDataStorage {

    /**
     * 포인트 날짜 폴더 생성
     * Create Point Date Folder
     *
     * 새로운 날짜 폴더를 생성합니다 (예: point/20240115/)
     * Creates a new date folder (e.g., point/20240115/)
     *
     * @return FileCreateResult - 성공 시 true, 실패 시 FileStorageError
     */
    fun createPointDate(): FileCreateResult

    /**
     * 포인트 데이터 폴더 생성
     * Create Point Data Folder
     *
     * 날짜 폴더 내에 순번 폴더를 생성합니다 (예: point/20240115/0/)
     * Creates a sequence folder within date folder (e.g., point/20240115/0/)
     *
     * @return FileCreateResult - 성공 시 true, 실패 시 FileStorageError
     */
    fun createPointData(): FileCreateResult

    /**
     * 포인트 위치 데이터 저장
     * Save Points Location Data
     *
     * LocationData 목록을 JSON 파일로 저장합니다
     * Saves LocationData list as JSON file
     *
     * @param locations 저장할 위치 데이터 목록
     */
    fun savePoints(locations: List<LocationData>)

    /**
     * 포인트 날짜 목록 조회
     * Fetch Point Date List
     *
     * 저장된 모든 날짜 폴더 목록을 조회합니다
     * Fetches list of all saved date folders
     *
     * @param completion 완료 콜백 - Result<List<PointDate>, FileStorageError>
     */
    fun fetchPointDateList(completion: (Result<List<PointDate>>) -> Unit)

    /**
     * 포인트 데이터 목록 조회
     * Fetch Point Data List
     *
     * 특정 날짜 폴더 내의 모든 순번 폴더 목록을 조회합니다
     * Fetches list of all sequence folders within a specific date folder
     *
     * @param pointDate 날짜 정보
     * @param completion 완료 콜백 - Result<List<PointData>, FileStorageError>
     */
    fun fetchPointDataList(pointDate: PointDate, completion: (Result<List<PointData>>) -> Unit)

    /**
     * 위치 데이터 목록 조회
     * Fetch Locations
     *
     * 특정 순번 폴더 내의 모든 JSON 파일을 읽어서 LocationData 목록을 반환합니다
     * Reads all JSON files within a specific sequence folder and returns LocationData list
     *
     * @param pointData 데이터 정보 (순번 폴더)
     * @param completion 완료 콜백 - Result<List<LocationData>, FileStorageError>
     */
    fun fetchLocations(pointData: PointData, completion: (Result<List<LocationData>>) -> Unit)
}

/**
 * 파일 데이터 저장소 구현체
 * File Data Storage Implementation
 *
 * iOS의 FileDataStorage class에 대응
 * FDFileManager를 사용하여 파일 시스템 작업 수행
 */
class FileDataStorageImpl(
    private val fileManager: com.bj.fishingdiary.managers.FDFileManager
) : FileDataStorage {

    override fun createPointDate(): FileCreateResult {
        return fileManager.createPointDate()
    }

    override fun createPointData(): FileCreateResult {
        return fileManager.createPointData()
    }

    override fun savePoints(locations: List<LocationData>) {
        fileManager.savePoints(locations)
    }

    override fun fetchPointDateList(completion: (Result<List<PointDate>>) -> Unit) {
        fileManager.fetchPointDateList(completion)
    }

    override fun fetchPointDataList(pointDate: PointDate, completion: (Result<List<PointData>>) -> Unit) {
        fileManager.fetchPointDataList(pointDate, completion)
    }

    override fun fetchLocations(pointData: PointData, completion: (Result<List<LocationData>>) -> Unit) {
        fileManager.fetchLocations(pointData, completion)
    }
}
