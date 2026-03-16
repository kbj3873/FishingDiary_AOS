package com.onbada.seathermo.data.repository

import android.content.Context
import com.onbada.seathermo.data.storage.FishingRecordDao
import com.onbada.seathermo.data.storage.FishingRecordEntity
import com.onbada.seathermo.domain.entity.FishingRecord
import com.onbada.seathermo.domain.entity.GeoPoint
import com.onbada.seathermo.domain.repository.FishingRecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * FishingRecordRepository 구현체.
 *
 * iOS의 DefaultFishingRecordRepository에 대응합니다.
 *
 * [개념] context는 파일 저장 경로(filesDir) 접근에 사용합니다.
 *        iOS의 FileManager.default와 동일한 역할을 Android Context가 담당합니다.
 *
 * [개념] repositoryScope는 DB/파일 I/O 전용 코루틴 스코프입니다.
 *        savePoint, savePhoto 같은 fire-and-forget 쓰기 작업에 사용합니다.
 *        SupervisorJob()은 자식 코루틴 하나가 실패해도 다른 코루틴에 영향을 주지 않습니다.
 *        iOS의 RealmManager.add()처럼 결과를 기다리지 않는 저장에 대응합니다.
 */
class DefaultFishingRecordRepository(
    private val dao: FishingRecordDao,
    private val context: Context,
    private val repositoryScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) : FishingRecordRepository {

    // ==================== Save ====================

    /**
     * 위치 및 속도 데이터를 포인트 1건으로 저장합니다.
     *
     * [개념] repositoryScope.launch { }는 fire-and-forget 비동기 실행입니다.
     *        호출 즉시 반환하고, DB 저장은 백그라운드에서 진행됩니다.
     *        iOS의 realmManager.add(record)에 대응합니다.
     */
    override fun savePoint(
        sessionId: String,
        latitude: Double,
        longitude: Double,
        speed: Double,
        state: Int,
        timestamp: Long
    ) {
        repositoryScope.launch {
            val entity = FishingRecordEntity(
                sessionId = sessionId,
                date = timestamp,
                latitude = latitude,
                longitude = longitude,
                speed = speed,
                state = state
            )
            dao.insert(entity)
        }
    }

    /**
     * 사진을 파일 시스템에 저장하고 DB에 기록합니다.
     *
     * [개념] context.filesDir은 앱 전용 내부 저장소 경로입니다.
     *        권한 없이 사용 가능하며, 앱 삭제 시 함께 제거됩니다.
     *        iOS의 FileManager.default.urls(for: .documentDirectory)에 대응합니다.
     *
     * [개념] File(directory, fileName)은 경로를 조합합니다.
     *        iOS의 documentsDirectory.appendingPathComponent(fileName)에 대응합니다.
     *
     * [개념] writeBytes()는 ByteArray를 파일에 저장합니다.
     *        iOS의 image.write(to: fileURL)에 대응합니다.
     *
     * @return 저장된 파일명. 실패 시 null.
     */
    override fun savePhoto(
        sessionId: String,
        imageData: ByteArray,
        timestamp: Long,
        location: GeoPoint,
        state: Int
    ): String? {
        return try {
            // 1. 파일 저장
            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            file.writeBytes(imageData)

            // 2. DB 저장 (fire-and-forget)
            repositoryScope.launch {
                val entity = FishingRecordEntity(
                    sessionId = sessionId,
                    date = timestamp,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    speed = 0.0,
                    state = state,
                    imagePaths = listOf(fileName)
                )
                dao.insert(entity)
            }

            fileName
        } catch (e: Exception) {
            // [개념] null을 반환해 실패를 상위로 전달합니다.
            //        iOS의 catch { return nil }에 대응합니다.
            null
        }
    }

    // ==================== Fetch ====================

    /**
     * 특정 날짜의 기록을 조회합니다.
     *
     * [개념] 하루의 시작/끝 타임스탬프를 계산해 범위 쿼리를 수행합니다.
     *        iOS의 Calendar.current.startOfDay(for: date)에 대응합니다.
     *
     *        86_400_000L = 24 * 60 * 60 * 1000 (1일 밀리초).
     *        Kotlin에서 숫자 리터럴에 _를 넣어 가독성을 높일 수 있습니다.
     */
    override suspend fun fetchRecords(date: Long): List<FishingRecord> {
        val startOfDay = toStartOfDay(date)
        val endOfDay = startOfDay + 86_400_000L
        return dao.getByDate(startOfDay, endOfDay).map { it.toDomain() }
    }

    /**
     * 전체 기록을 최신순으로 조회합니다 (히스토리 화면용).
     */
    override suspend fun fetchAllRecords(): List<FishingRecord> {
        return dao.getAll().map { it.toDomain() }
    }

    // ==================== Delete ====================

    override fun deleteSession(sessionId: String) {
        repositoryScope.launch {
            dao.deleteBySession(sessionId)
        }
    }

    override fun deleteFishingRecords(ids: List<String>) {
        repositoryScope.launch {
            dao.deleteByIds(ids)
        }
    }

    override fun deleteFishingRecord(id: String) {
        repositoryScope.launch {
            dao.deleteById(id)
        }
    }

    // ==================== Private ====================

    /**
     * Unix timestamp를 해당 날짜 00:00:00.000 타임스탬프로 변환합니다.
     *
     * [개념] %는 나머지 연산자입니다. timestamp에서 하루 단위 나머지를 빼면 해당 날의 시작이 됩니다.
     *        단, 이 방식은 UTC 기준입니다. 한국 시간(KST = UTC+9)으로 처리하려면
     *        TimeZone 오프셋을 추가해야 합니다.
     */
    private fun toStartOfDay(timestamp: Long): Long {
        val oneDayMs = 86_400_000L
        val timezoneOffsetMs = java.util.TimeZone.getDefault().getOffset(timestamp).toLong()
        return ((timestamp + timezoneOffsetMs) / oneDayMs) * oneDayMs - timezoneOffsetMs
    }
}
