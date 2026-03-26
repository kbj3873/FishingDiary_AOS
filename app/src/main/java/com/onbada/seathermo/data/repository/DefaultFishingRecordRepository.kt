package com.onbada.seathermo.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
     * 카메라가 저장한 원본 파일로부터 썸네일을 생성하고 DB에 기록합니다.
     *
     * [개념] TakePicture 계약은 카메라 앱이 filesDir의 uuid.jpg에 직접 원본을 저장합니다.
     *        이 메서드는 원본(uuid.jpg)을 읽어 축소된 썸네일(uuid_thumb.jpg)을 생성합니다.
     *        - 원본(uuid.jpg): 이미지 뷰어에서 고해상도로 표시
     *        - 썸네일(uuid_thumb.jpg): 지도 마커에서 빠르게 로드
     *        DB에는 원본 파일명(uuid.jpg)만 기록합니다.
     *
     * [개념] context.filesDir은 앱 전용 내부 저장소 경로입니다.
     *        iOS의 FileManager.default.urls(for: .documentDirectory)에 대응합니다.
     *
     * @return 원본 파일명(uuid.jpg). 저장 실패 시 null.
     */
    override fun savePhoto(
        sessionId: String,
        fileName: String,
        timestamp: Long,
        location: GeoPoint,
        state: Int
    ): String? {
        return try {
            // 1. 원본 파일 확인 (카메라가 이미 저장한 파일)
            val originalFile = File(context.filesDir, fileName)
            if (!originalFile.exists()) return null

            // 2. 썸네일 생성 및 저장.
            // [개념] BitmapFactory.decodeFile은 파일 경로로부터 Bitmap을 로드합니다.
            //        iOS의 UIImage(contentsOfFile:)에 대응합니다.
            val originalBitmap = BitmapFactory.decodeFile(originalFile.absolutePath)
            if (originalBitmap != null) {
                // 긴 변을 300px로 맞춰 종횡비를 유지하며 축소합니다.
                // [개념] minOf는 두 값 중 작은 값을 반환합니다. Swift의 min()과 동일합니다.
                val thumbMaxSize = 300f
                val scale = minOf(
                    thumbMaxSize / originalBitmap.width,
                    thumbMaxSize / originalBitmap.height
                )
                val thumbWidth  = (originalBitmap.width  * scale).toInt().coerceAtLeast(1)
                val thumbHeight = (originalBitmap.height * scale).toInt().coerceAtLeast(1)

                // [개념] Bitmap.createScaledBitmap은 지정한 크기로 비트맵을 리사이즈합니다.
                //        3번째 인자 true는 bilinear filtering으로 화질 저하를 줄입니다.
                val thumbBitmap = Bitmap.createScaledBitmap(originalBitmap, thumbWidth, thumbHeight, true)

                // 썸네일 파일명 파생: uuid.jpg → uuid_thumb.jpg
                val thumbFileName = fileName.removeSuffix(".jpg") + "_thumb.jpg"
                val thumbFile = File(context.filesDir, thumbFileName)

                // [개념] use { }는 블록 종료 시 스트림을 자동으로 close()합니다.
                //        Swift의 defer { stream.close() }에 대응합니다.
                thumbFile.outputStream().use { out ->
                    thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
                }
                thumbBitmap.recycle()
                originalBitmap.recycle()
            }

            // 3. DB 저장 (fire-and-forget). 원본 파일명을 기록합니다.
            repositoryScope.launch {
                dao.insert(FishingRecordEntity(
                    sessionId  = sessionId,
                    date       = timestamp,
                    latitude   = location.latitude,
                    longitude  = location.longitude,
                    speed      = 0.0,
                    state      = state,
                    imagePaths = listOf(fileName)
                ))
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
