package com.onbada.seathermo.domain.repository

import com.onbada.seathermo.domain.entity.FishingRecord
import com.onbada.seathermo.domain.entity.GeoPoint

/**
 * 낚시 기록 Repository 인터페이스.
 *
 * 낚시 세션 중 수집되는 위치 기록과 사진을 저장·조회·삭제합니다.
 * iOS의 FishingRecordRepository protocol에 대응합니다.
 *
 * [개념] 읽기 작업(fetch*)은 suspend fun으로 선언해 비동기 처리합니다.
 *        쓰기/삭제 작업(save*, delete*)은 Room DB에 직접 접근하므로
 *        구현체에서 필요에 따라 suspend 또는 백그라운드 스레드를 선택합니다.
 *        여기서는 iOS와 동일하게 일반 fun으로 선언합니다.
 */
interface FishingRecordRepository {

    /**
     * 위치 및 속도 데이터를 포인트 1건으로 저장합니다.
     *
     * [개념] iOS의 Date → Android Long (Unix timestamp, milliseconds).
     *        Long은 64비트 정수형으로 Swift의 Int64와 동일합니다.
     *
     * @param sessionId 낚시 세션 식별자.
     * @param latitude 위도.
     * @param longitude 경도.
     * @param speed 속도 (km/h).
     * @param state 포인트 상태 (0: 이동 중, 1: 포인트 정박).
     * @param timestamp 측정 시각 (Unix timestamp, milliseconds).
     */
    fun savePoint(
        sessionId: String,
        latitude: Double,
        longitude: Double,
        speed: Double,
        state: Int,
        timestamp: Long
    )

    /**
     * 사진을 저장하고 저장된 파일 경로를 반환합니다.
     *
     * [개념] iOS의 Data → Android ByteArray.
     *        iOS의 (Double, Double) tuple → Android GeoPoint data class.
     *        Kotlin에는 tuple이 없어 두 값을 묶는 data class를 사용합니다.
     *
     * @param sessionId 낚시 세션 식별자.
     * @param imageData 저장할 이미지 바이트 데이터.
     * @param timestamp 촬영 시각 (Unix timestamp, milliseconds).
     * @param location 촬영 위치 (위도, 경도).
     * @param state 포인트 상태.
     * @return 저장된 이미지 파일 경로. 저장 실패 시 null.
     *         [개념] String? 는 nullable 타입입니다. Swift의 String?와 동일합니다.
     */
    fun savePhoto(
        sessionId: String,
        imageData: ByteArray,
        timestamp: Long,
        location: GeoPoint,
        state: Int
    ): String?

    /**
     * 특정 날짜의 낚시 기록 목록을 가져옵니다.
     *
     * @param date 조회할 날짜 (Unix timestamp, milliseconds, 해당 날짜의 시작 시각).
     * @return 해당 날짜의 낚시 기록 목록.
     */
    suspend fun fetchRecords(date: Long): List<FishingRecord>

    /**
     * 전체 낚시 기록 목록을 가져옵니다 (히스토리 화면용).
     *
     * @return 저장된 모든 낚시 기록 목록.
     */
    suspend fun fetchAllRecords(): List<FishingRecord>

    /**
     * 특정 세션의 모든 기록을 삭제합니다.
     *
     * @param sessionId 삭제할 세션 식별자.
     */
    fun deleteSession(sessionId: String)

    /**
     * 여러 낚시 기록을 ID 목록으로 일괄 삭제합니다.
     *
     * @param ids 삭제할 기록 ID 목록.
     */
    fun deleteFishingRecords(ids: List<String>)

    /**
     * 단일 낚시 기록을 삭제합니다 (사진 마커 개별 삭제용).
     *
     * @param id 삭제할 기록 ID.
     */
    fun deleteFishingRecord(id: String)
}
