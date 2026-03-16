package com.onbada.seathermo.domain.usecase

import com.onbada.seathermo.domain.entity.FishingRecord
import com.onbada.seathermo.domain.entity.GeoPoint
import com.onbada.seathermo.domain.repository.FishingRecordRepository

/**
 * 낚시 기록 UseCase 인터페이스.
 *
 * iOS의 FishingRecordUseCase protocol에 대응합니다.
 *
 * [개념] Kotlin interface는 추상 메서드뿐 아니라 기본 구현(default method)도 가질 수 있습니다.
 *        Swift의 protocol extension과 동일한 역할을 합니다.
 *        timestamp 파라미터의 기본값을 interface 레벨에서 제공할 수 있습니다.
 */
interface FishingRecordUseCase {

    /**
     * 위치 및 속도를 포인트 1건으로 저장합니다.
     *
     * [개념] Long = 타임스탬프 기본값은 구현체에서 System.currentTimeMillis()로 처리합니다.
     *        iOS extension의 savePoint(... timestamp: Date = Date())에 대응합니다.
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
     * timestamp 생략 시 현재 시각을 자동 사용하는 기본 구현.
     *
     * [개념] interface의 default method입니다.
     *        Swift의 protocol extension func savePoint(...) { savePoint(..., timestamp: Date()) }와 동일합니다.
     *        [개념] System.currentTimeMillis()는 Unix epoch 기준 현재 시각(ms)을 반환합니다.
     *              Swift의 Date().timeIntervalSince1970 * 1000과 동일합니다.
     */
    fun savePoint(
        sessionId: String,
        latitude: Double,
        longitude: Double,
        speed: Double,
        state: Int
    ) {
        savePoint(
            sessionId = sessionId,
            latitude = latitude,
            longitude = longitude,
            speed = speed,
            state = state,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * 사진을 저장하고 저장된 파일 경로를 반환합니다.
     *
     * @return 저장 경로. 실패 시 null.
     */
    fun savePhoto(
        sessionId: String,
        imageData: ByteArray,
        location: GeoPoint,
        state: Int
    ): String?

    suspend fun fetchRecords(date: Long): List<FishingRecord>
    suspend fun fetchAllRecords(): List<FishingRecord>
    fun deleteSession(sessionId: String)
    fun deleteFishingRecord(id: String)
    fun deleteFishingRecords(ids: List<String>)
}

/**
 * FishingRecordUseCase 기본 구현체.
 *
 * iOS의 DefaultFishingRecordUseCase에 대응합니다.
 */
class DefaultFishingRecordUseCase(
    private val repository: FishingRecordRepository
) : FishingRecordUseCase {

    override fun savePoint(
        sessionId: String,
        latitude: Double,
        longitude: Double,
        speed: Double,
        state: Int,
        timestamp: Long
    ) {
        repository.savePoint(
            sessionId = sessionId,
            latitude = latitude,
            longitude = longitude,
            speed = speed,
            state = state,
            timestamp = timestamp
        )
    }

    override fun savePhoto(
        sessionId: String,
        imageData: ByteArray,
        location: GeoPoint,
        state: Int
    ): String? {
        // [개념] System.currentTimeMillis()로 현재 시각을 Repository에 전달합니다.
        //        iOS의 timestamp: Date()와 동일하게 호출 시점의 시각을 사용합니다.
        return repository.savePhoto(
            sessionId = sessionId,
            imageData = imageData,
            timestamp = System.currentTimeMillis(),
            location = location,
            state = state
        )
    }

    override suspend fun fetchRecords(date: Long): List<FishingRecord> {
        return repository.fetchRecords(date)
    }

    override suspend fun fetchAllRecords(): List<FishingRecord> {
        return repository.fetchAllRecords()
    }

    override fun deleteSession(sessionId: String) {
        repository.deleteSession(sessionId)
    }

    override fun deleteFishingRecord(id: String) {
        repository.deleteFishingRecord(id)
    }

    override fun deleteFishingRecords(ids: List<String>) {
        repository.deleteFishingRecords(ids)
    }
}
